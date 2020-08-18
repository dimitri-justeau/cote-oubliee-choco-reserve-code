package baseproblem;

import chocoreserve.raster.RasterReader;
import chocoreserve.solver.feature.QuantitativeFeature;
import chocoreserve.solver.feature.raster.QuantitativeRasterFeature;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;

public class Data {

    public static final String BASEPATH = Data.class.getResource("/data").getPath();
    public static final String FOREST_BINARY_RASTER_PATH = Paths.get(BASEPATH, "forest_binary_480x480_0.7_wgs84_full_extent_cut.tif").toString();
    public static final String RESTORABLE_AREA_RASTER_PATH = Paths.get(BASEPATH, "restaurable_area_480x480_wgs84_ha.tif").toString();
    public static final String BUFFER_PISTES_PATH = Paths.get(BASEPATH, "buffer_1000m_wgs84_all_touched.tif").toString();

    public static final int BORENDY_RASTER_VALUE = 1;
    public static final int UNIA_RASTER_VALUE = 2;

    public QuantitativeFeature forest_binary, restorable_area, buffer_pistes;
    public  int[] forest_binary_data, restorable_area_data, buffer_data;
    public int width, height;

    public Data() throws IOException {
        RasterReader raster_forest_binary = new RasterReader(FOREST_BINARY_RASTER_PATH);
        RasterReader raster_restaurable_area = new RasterReader(RESTORABLE_AREA_RASTER_PATH);
        RasterReader raster_buffer_pistes = new RasterReader(BUFFER_PISTES_PATH);
        height = raster_forest_binary.getHeight();
        width = raster_forest_binary.getWidth();
        assert height == raster_restaurable_area.getHeight();
        assert height == raster_buffer_pistes.getHeight();
        assert width == raster_buffer_pistes.getHeight();
        assert width == raster_restaurable_area.getHeight();
        forest_binary = new QuantitativeRasterFeature(FOREST_BINARY_RASTER_PATH);
        restorable_area = new QuantitativeRasterFeature(RESTORABLE_AREA_RASTER_PATH);
        buffer_pistes = new QuantitativeRasterFeature(BUFFER_PISTES_PATH);
        forest_binary_data = forest_binary.getQuantitativeData();
        restorable_area_data = restorable_area.getQuantitativeData();
        buffer_data = buffer_pistes.getQuantitativeData();
    }

    public static void main(String[] args) throws IOException {
        Data data = new Data();
        System.out.println(data.forest_binary.getName());
        System.out.println(data.width);
        System.out.println(data.height);
        System.out.println(Arrays.toString(data.restorable_area.getQuantitativeData()));
    }
}
