package baseproblem;

import chocoreserve.grid.regular.square.PartialRegularSquareGrid;

import java.io.*;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.IntStream;

public class SolutionExporter {

    public static final String PYTHON_SCRIPT = SolutionExporter.class.getResource("../results").getPath() + "/generate_raster.py";
    public static final String TEMPLATE_PATH = SolutionExporter.class.getResource("../results").getPath() + "/template.tif";

    public String resultPath, fileName;
    public int[] solution;
    public int[] completeData;

    public SolutionExporter(String resultPath, String fileName) throws IOException {
        this.resultPath = resultPath;
        this.fileName = fileName;
        try (BufferedReader br = new BufferedReader(new FileReader(Paths.get(resultPath, fileName).toString()))) {
            String line = br.readLine();
            String[] values = line.split(",");
            solution = Arrays.stream(values).mapToInt(s -> Integer.parseInt(s)).toArray();
        }
        Data data = new Data();

        int[] forest_data = data.forest_binary.getQuantitativeData();

        int[] outPixels = IntStream.range(0, forest_data.length)
                .filter(i -> forest_data[i] == -1 ? true : false)
                .toArray();

        PartialRegularSquareGrid grid = new PartialRegularSquareGrid(data.height, data.width, outPixels);

        int[] nonForestPixels = IntStream.range(0, forest_data.length)
                .filter(i -> forest_data[i] == 0 ? true : false)
                .map(i -> grid.getPartialIndex(i))
                .toArray();

        int[] forestPixels = IntStream.range(0, forest_data.length)
                .filter(i -> forest_data[i] == 1 ? true : false)
                .map(i -> grid.getPartialIndex(i))
                .toArray();

        completeData = new int[forest_data.length];
        for (int i = 0; i < completeData.length; i++) {
            if (grid.getDiscardSet().contains(i)) {
                completeData[i] = -1;
            } else {
                completeData[i] = solution[grid.getPartialIndex(i)];
            }
        }
    }

    public void exportCompleteCsv() {
        BufferedWriter br = null;
        try {
            br = new BufferedWriter(new FileWriter(Paths.get(resultPath, "complete_" + fileName).toString()));
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

    public void generateRaster() {
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

    public static void main(String[] args) throws IOException {
        BaseProblem baseProblem = new BaseProblem("IICUnia");
        SolutionExporter exp = new SolutionExporter(baseProblem.resultsPath, "UniaOptimalIIC_3" + ".csv");
        exp.exportCompleteCsv();
        exp.generateRaster();
    }
}
