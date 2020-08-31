package restopt;

import chocoreserve.grid.regular.square.PartialRegularSquareGrid;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageBuilder;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.data.DataSourceException;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.opengis.parameter.GeneralParameterValue;

import java.awt.*;
import java.awt.image.*;
import java.awt.image.renderable.RenderedImageFactory;
import java.io.*;
import java.nio.file.Paths;
import java.util.Arrays;


public class SolutionExporter {

//    public static final String PYTHON_SCRIPT = SolutionExporter.class.getResource("../results").getPath() + "/generate_raster.py";

    public String csvDest, rastDest, template;
    public int[] solution;
    public int[] completeData;
    public BaseProblem baseProblem;

    public SolutionExporter(BaseProblem baseProblem, int[] solution, String template, String csvDest, String rastDest) {
        this.baseProblem = baseProblem;
        this.solution = solution;
        this.template = template;
        this.csvDest = csvDest;
        this.rastDest = rastDest;
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

    public void exportCompleteCsv() {
        BufferedWriter br = null;
        try {
            br = new BufferedWriter(new FileWriter(csvDest));
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

    public void exportCharacteristics(String[][] characteristic) throws IOException {
        BufferedWriter br = new BufferedWriter(new FileWriter(csvDest));
        StringBuilder sb = new StringBuilder();
        for (String[] line : characteristic) {
            int i = 0;
            for (String s : line) {
                i++;
                sb.append(s);
                if (i < line.length) {
                    sb.append(",");
                }
            }
            sb.append("\n");
        }
        br.write(sb.toString());
        br.close();
    }

    public void generateRaster() throws IOException {
        File file = new File(template);
        GeoTiffReader reader = new GeoTiffReader(file);
        GridCoverage2D grid = reader.read((GeneralParameterValue[])null);
        int height = grid.getRenderedImage().getHeight();
        int width = grid.getRenderedImage().getWidth();

        GeoTiffWriter writer = new GeoTiffWriter(new File(rastDest));

        DataBuffer buff = grid.getRenderedImage().getData().getDataBuffer();
        SampleModel sm = new BandedSampleModel(DataBuffer.TYPE_INT, width, height, 1);
        WritableRaster rast = Raster.createWritableRaster(sm, buff, new Point(0,0));
        rast.setPixels(0, 0, width, height, completeData);

        GridCoverageFactory f = new GridCoverageFactory();
        GridCoverage2D destCov = f.create("rast", rast, grid.getEnvelope());

        writer.write(destCov,null);
    }
}
