package restopt;

import chocoreserve.raster.RasterReader;
import chocoreserve.solver.feature.QuantitativeFeature;
import chocoreserve.solver.feature.raster.QuantitativeRasterFeature;

import java.io.IOException;

public class Data {

    public QuantitativeFeature habitat_binary, restorable_area, accessible_areas;
    public  int[] habitat_binary_data, restorable_area_data, accessible_areas_data;
    public int width, height;

    public String habitatBinaryRasterPath;
    public String accessibleAreasRasterPath;;
    public String restorableAreasRasterPath;

    public Data(String habitatBinaryRasterPath, String accessibleAreasRasterPath, String restorableAreaRasterPath) throws IOException {
        this.habitatBinaryRasterPath = habitatBinaryRasterPath;
        this.accessibleAreasRasterPath = accessibleAreasRasterPath;
        this.restorableAreasRasterPath = restorableAreaRasterPath;
        RasterReader raster_forest_binary = new RasterReader(habitatBinaryRasterPath);
        RasterReader raster_restorable_area = new RasterReader(restorableAreaRasterPath);
        RasterReader raster_accessible_areas = new RasterReader(accessibleAreasRasterPath);
        height = raster_forest_binary.getHeight();
        width = raster_forest_binary.getWidth();
        assert height == raster_restorable_area.getHeight();
        assert height == raster_accessible_areas.getHeight();
        assert width == raster_restorable_area.getHeight();
        assert width == raster_accessible_areas.getHeight();
        habitat_binary = new QuantitativeRasterFeature(habitatBinaryRasterPath);
        restorable_area = new QuantitativeRasterFeature(restorableAreaRasterPath);
        accessible_areas = new QuantitativeRasterFeature(accessibleAreasRasterPath);
        habitat_binary_data = habitat_binary.getQuantitativeData();
        restorable_area_data = restorable_area.getQuantitativeData();
        accessible_areas_data = accessible_areas.getQuantitativeData();
    }
}
