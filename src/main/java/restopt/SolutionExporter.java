package restopt;

import chocoreserve.grid.regular.square.PartialRegularSquareGrid;

import java.io.*;
import java.nio.file.Paths;


public class SolutionExporter {

    public static final String PYTHON_SCRIPT = SolutionExporter.class.getResource("../results").getPath() + "/generate_raster.py";
    public static final String TEMPLATE_PATH = SolutionExporter.class.getResource("../results").getPath() + "/template.tif";

    public String resultPath, fileName;
    public int[] solution;
    public int[] completeData;
    public BaseProblem baseProblem;

    public SolutionExporter(BaseProblem baseProblem, int[] solution, String fileName) throws IOException {
        this.baseProblem = baseProblem;
        this.solution = solution;
        PartialRegularSquareGrid grid = baseProblem.grid;
        completeData = new int[grid.getNbRows() * grid.getNbCols()];
        for (int i = 0; i < completeData.length; i++) {
            if (grid.getDiscardSet().contains(i)) {
                completeData[i] = -1;
            } else {
                completeData[i] = solution[grid.getPartialIndex(i)];
            }
        }
    }

    public void exportCompleteCsv(String dest) {
        BufferedWriter br = null;
        try {
            br = new BufferedWriter(new FileWriter(Paths.get(dest).toString()));
            StringBuilder sb = new StringBuilder();

            // Append strings from array
            for (int element : completeData) {
                sb.append(element);
                sb.append(",");
            }

            br.write(sb.toString());
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void generateRaster(String dest) {
        try {
            Runtime.getRuntime().exec(
                    "python3 " +
                            PYTHON_SCRIPT + " " +
                            TEMPLATE_PATH + " " +
                            Paths.get(resultPath, "complete_" + fileName).toString() + " " +
                    Paths.get(resultPath, "complete_" + fileName + ".tif").toString()
            );
        } catch (IOException e) {
            System.err.println("Error while exporting solution raster.");
            System.err.println(e.toString());
        }
    }
}
