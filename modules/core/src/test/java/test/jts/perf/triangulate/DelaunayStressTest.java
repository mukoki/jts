/*
 * Copyright (c) 2023 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package test.jts.perf.triangulate;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.algorithm.ConvexHull;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.operation.overlayng.CoverageUnion;
import org.locationtech.jts.triangulate.DelaunayTriangulationBuilder;
import org.locationtech.jts.triangulate.VoronoiDiagramBuilder;
import org.locationtech.jts.util.Memory;
import org.locationtech.jts.util.Stopwatch;

/**
 * Test correctness of Delaunay computation with 
 * synthetic random datasets.
 * 
 * @author Martin Davis
 *
 */
public class DelaunayStressTest 
{
  private static final int N_PTS = 50;
  private static final int RUN_COUNT = 10000;
  final static double SIDE_LEN = 1000.0;
  final static double BASE_OFFSET = 0;

  public static void main(String args[]) {
  	DelaunayStressTest test = new DelaunayStressTest();
  	test.run();
  }
	
	final static GeometryFactory geomFact = new GeometryFactory();
  private static final double WIDTH = 100;
  private static final double HEIGHT = 100;
	
	
  public void run()
  {
    for (int i = 0; i < RUN_COUNT; i++) {
      System.out.println("Run # " + i);
      run(N_PTS);
    }
  }
  
	public void run(int nPts)
	{
    List<Coordinate> pts = randomPointsInGrid(nPts, BASE_OFFSET, BASE_OFFSET, WIDTH, HEIGHT, 1);
    run(pts);
	}
	
	public void run(List<Coordinate> pts)
	{
    System.out.println("Base offset: " + BASE_OFFSET);
		System.out.println("# pts: " + pts.size());
		Stopwatch sw = new Stopwatch();
		DelaunayTriangulationBuilder builder = new DelaunayTriangulationBuilder();
		builder.setSites(pts);
		
		Geometry tris = builder.getTriangles(geomFact);
		checkDelaunay(tris);
		
		checkVoronoi(pts);
		
		System.out.println("  --  Time: " + sw.getTimeString()
				+ "  Mem: " + Memory.usedTotalString());
//		System.out.println(g);
	}
	
	private void checkVoronoi(List<Coordinate> pts) {
	  VoronoiDiagramBuilder vdb = new VoronoiDiagramBuilder();
	  vdb.setSites(pts);
	  vdb.getDiagram(geomFact);
	  
	  //-- for now simply confirm the Voronoi is computed with no failure
  }

  private void checkDelaunay(Geometry tris) {
	  //TODO: check all elements are triangles
	  
	  //-- check triangulation is a coverage
	  //-- this will error if triangulation is not a valid coverage
	  Geometry union = CoverageUnion.union(tris);
	  
	  checkConvex(tris, union);
  }

  private void checkConvex(Geometry tris, Geometry triHull) {
    Geometry convexHull = convexHull(tris); 
    boolean isEqual = triHull.equalsTopo(convexHull);
    
    boolean isConvex = isConvex((Polygon) triHull);
	  
	  if (! isConvex) {
      System.out.println("Tris:");
      System.out.println(tris);
      System.out.println("Convex Hull:");
      System.out.println(convexHull);
	    throw new IllegalStateException("Delaunay triangulation is not convex");
	  }
  }

  private Geometry convexHull(Geometry tris) {
    ConvexHull hull = new ConvexHull(tris);
    return hull.getConvexHull();
  }

  private boolean isConvex(Polygon poly) {
    Coordinate[] pts = poly.getCoordinates();
    for (int i = 0; i < pts.length - 1; i++) {
      int iprev = i - 1;
      if (iprev < 0) iprev = pts.length - 2;
      int inext = i + 1;
      //-- orientation must be CLOCKWISE or COLLINEAR
      boolean isConvex = Orientation.COUNTERCLOCKWISE != Orientation.index(pts[iprev], pts[i], pts[inext]);
      if (! isConvex)
        return false;
    }
    return true;
  }
  static List<Coordinate> randomPointsInGrid(int nPts, double basex, double basey, double width, double height, double scale)
	{
    PrecisionModel pm = null;
    if (scale > 0) {
      pm = new PrecisionModel(scale);
    }
		List<Coordinate> pts = new ArrayList<Coordinate>();
		
		int nSide = (int) Math.sqrt(nPts) + 1;
		
		for (int i = 0; i < nSide; i++) {
			for (int j = 0; j < nSide; j++) {
				double x = basex + i * width + width * Math.random();
				double y = basey + j * height + height * Math.random();
				Coordinate p = new Coordinate(x, y);
				round(p, pm);
				pts.add(p);
			}
		}
		return pts;
	}
	
	private static void round(Coordinate p, PrecisionModel pm) {
	  if (pm == null)
	    return;
	  pm.makePrecise(p);
  }

  static List<Coordinate> randomPoints(int nPts, double sideLen)
	{
		List<Coordinate> pts = new ArrayList<Coordinate>();
		
		for (int i = 0; i < nPts; i++) {
				double x = sideLen * Math.random();
				double y = sideLen * Math.random();
				pts.add(new Coordinate(x, y));
		}
		return pts;
	}
}
