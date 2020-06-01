# coding: utf-8



if __name__ == "__main__":

    import sys
    import os

    import rasterio
    import numpy as np

    template_path = sys.argv[1]
    complete_csv_path = sys.argv[2]
    dest_raster_path = sys.argv[3]

    data = np.fromfile(complete_csv_path, dtype=float, sep=",")

    template = rasterio.open(template_path)
    meta = template.meta.copy()
    meta.pop('nodata')

    with rasterio.open(dest_raster_path, "w", **meta) as out:
        a = template.read(1)
        for i in range(len(data)):
            y, x = np.unravel_index(i, a.shape, order="C")
            a[y][x] = data[i]
        out.write_band(1, a)
