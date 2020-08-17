package baseproblem;

import chocoreserve.grid.neighborhood.Neighborhoods;
import chocoreserve.grid.regular.square.PartialRegularSquareGrid;
import chocoreserve.grid.regular.square.RegularSquareGrid;
import chocoreserve.solver.ReserveModel;
import chocoreserve.solver.region.ComposedRegion;
import chocoreserve.solver.region.Region;
import chocoreserve.solver.variable.SpatialGraphVar;
import org.chocosolver.graphsolver.GraphModel;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.setDataStructures.SetType;

import java.io.*;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.IntStream;

public class BaseProblemBorendy {

    public String name;
    public Data data;
    public String resultsPath;
    public RegularSquareGrid grid;
    public SpatialGraphVar potentialForestGraphVar;
    public ReserveModel reserveModel;

    public Region nonForest, forest, reforestUnia, reforestBorendy;
    public ComposedRegion potentialForest;

    public IntVar  minReforestAreaBorendy,maxReforestAreaBorendy;

    public BaseProblemBorendy(String name) throws IOException {
        this.name = name;
        this.data = new Data();
        File f = new File(this.getClass().getResource("../results").getPath() + "/" + name);
        f.mkdir();
        this.resultsPath = this.getClass().getResource("../results/" + name).getPath();

        System.out.println("Height = " + data.height);
        System.out.println("Width = " + data.width);

        System.out.println(data.forest_binary_data.length);
        System.out.println(data.buffer_data.length);

        int[] outPixels = IntStream.range(0, data.forest_binary_data.length)
                .filter(i -> data.forest_binary_data[i] <= -1)
                .toArray();

        this.grid = new RegularSquareGrid(data.height, data.width);

        int[] nonForestPixels = IntStream.range(0, data.forest_binary_data.length)
                .filter(i -> data.forest_binary_data[i] == 0)
//                .map(i -> grid.getPartialIndex(i))
                .toArray();

        int[] forestPixels = IntStream.range(0, data.forest_binary_data.length)
                .filter(i -> data.forest_binary_data[i] == 1)
//                .map(i -> grid.getPartialIndex(i))
                .toArray();

//        int[] restorableAreaPixels = IntStream.of(nonForestPixels)
//                .map(i -> data.restorable_area_data[grid.getCompleteIndex(i)])
//                .toArray();

        int[] nonForestBufferUniaPixels = IntStream.range(0, data.buffer_data.length)
                .filter(i -> data.buffer_data[i] == Data.UNIA_RASTER_VALUE && data.forest_binary_data[i] == 0)
//                .map(i -> grid.getPartialIndex(i))
                .toArray();

        int[] nonForestBufferBorendyPixels = IntStream.range(0, data.buffer_data.length)
                .filter(i -> data.buffer_data[i] == Data.BORENDY_RASTER_VALUE && data.forest_binary_data[i] == 0)
//                .map(i -> grid.getPartialIndex(i))
                .toArray();

        int[] nonForestBufferPixels = IntStream.range(0, data.buffer_data.length)
                .filter(i -> (data.buffer_data[i] == Data.UNIA_RASTER_VALUE || data.buffer_data[i] == Data.BORENDY_RASTER_VALUE) && data.forest_binary_data[i] == 0)
//                .map(i -> grid.getPartialIndex(i))
                .toArray();

        int[] nonForestNonBufferPixels = IntStream.range(0, data.forest_binary_data.length)
                .filter(i -> data.forest_binary_data[i] == 0 && data.buffer_data[i] <= 0)
//                .map(i -> grid.getPartialIndex(i))
                .toArray();

        System.out.println("Current landscape state loaded");
        System.out.println("    forest sites = " + forestPixels.length + " ");
        System.out.println("    non forest sites = " + nonForestPixels.length + " ");
        System.out.println("    non forest sites in buffer Unia = " + nonForestBufferUniaPixels.length + " ");
        System.out.println("    non forest sites in buffer Borendy = " + nonForestBufferBorendyPixels.length + " ");
        System.out.println("    out sites = " + outPixels.length);

        forest = new Region(
                "forest",
                Neighborhoods.FOUR_CONNECTED,
                SetType.SMALLBIPARTITESET,
                forestPixels,
                forestPixels
        );
        nonForest = new Region(
                "nonForest",
                Neighborhoods.FOUR_CONNECTED,
                SetType.SMALLBIPARTITESET,
                nonForestNonBufferPixels,
                nonForestPixels
        );
        reforestUnia = new Region(
                "reforest",
                Neighborhoods.FOUR_CONNECTED,
                SetType.SMALLBIPARTITESET,
                new int[] {},
                nonForestBufferUniaPixels
        );

        reforestBorendy = new Region(
                "reforest",
                Neighborhoods.FOUR_CONNECTED,
                SetType.SMALLBIPARTITESET,
                new int[] {},
                nonForestBufferBorendyPixels
        );

        potentialForest = new ComposedRegion("potentialForest", forest, reforestBorendy);

        System.out.println("Regions created");

        this.reserveModel = new ReserveModel(
                grid,
                new Region[] {nonForest, forest, reforestBorendy},
                new ComposedRegion[] {potentialForest}
        );

        int nbSites = reserveModel.getSites().length;

        GraphModel model = reserveModel.getChocoModel();

        potentialForestGraphVar = potentialForest.getSetVar();

        // Connectivity constraints on reforest regions
        reserveModel.nbConnectedComponents(reforestBorendy, 1, 1).post();

        // Minimum area to ensure every site to >= 0.7 forest proportion (in ha)
        int[] minArea = new int[nbSites];
        int[] maxRestorableArea = new int[nbSites];
        for (int i = 0; i < nbSites; i++) {
//            int restorable = data.restorable_area_data[grid.getCompleteIndex(i)];
            int restorable = data.restorable_area_data[i];
            minArea[i] = restorable <= 7 ? 0 : restorable - 7;
            maxRestorableArea[i] = restorable;
        }

        // Borendy restorable area constraint
        minReforestAreaBorendy = model.intVar(90, 110);
        maxReforestAreaBorendy = model.intVar(90, 10000);
        model.sumElements(reforestBorendy.getSetVar(), minArea, minReforestAreaBorendy).post();
        model.sumElements(reforestBorendy.getSetVar(), maxRestorableArea, maxReforestAreaBorendy).post();

        // Radius constraint for Borendy area
        reserveModel.maxDiameterSpatial(reforestBorendy, 6).post();

    }

    public void saveSolution(String name) throws IOException {
        String path = Paths.get(resultsPath, name + ".csv").toString();
        int[] solution = IntStream.range(0, reserveModel.getSites().length)
                .map(j -> reserveModel.getSites()[j].getValue())
                .toArray();

        BufferedWriter br = null;
        try {
            br = new BufferedWriter(new FileWriter(path));
            StringBuilder sb = new StringBuilder();

            // Append strings from array
            for (int element : solution) {
                sb.append(element);
                sb.append(",");
            }

            br.write(sb.toString());
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        SolutionExporter exp = new SolutionExporter(resultsPath, name + ".csv");
        exp.exportCompleteCsv();
        exp.generateRaster();
    }

    public static void main(String[] args) throws IOException {
        BaseProblemBorendy baseProblem = new BaseProblemBorendy("IIC_Borendy");
        String path = Paths.get(baseProblem.resultsPath, "IICOptimalBorendy6" + ".csv").toString();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line = br.readLine();
            String[] values = line.split(",");
            int[] solution = Arrays.stream(values).mapToInt(s -> Integer.parseInt(s)).toArray();
            int[] borendy = IntStream.range(0, values.length).filter(i -> solution[i] == 2).toArray();
            System.out.println(Arrays.toString(borendy));
        }
    }
}
