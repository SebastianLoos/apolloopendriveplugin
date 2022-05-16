package org.openstreetmap.josm.plugins.apolloopendrive.toosm;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.AbstractReader;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.plugins.apolloopendrive.ApolloOpenDriveReader;
import org.openstreetmap.josm.plugins.apolloopendrive.xml.Lane;
import org.openstreetmap.josm.plugins.apolloopendrive.xml.OpenDRIVE;
import org.openstreetmap.josm.plugins.apolloopendrive.xml.OpenDRIVE.Junction;
import org.openstreetmap.josm.plugins.apolloopendrive.xml.OpenDRIVE.Road;

public class ApolloOpenDriveToOsmReader extends AbstractReader {

	private OpenDRIVE readOpenDriveData(InputStream input) throws JAXBException {
		JAXBContext jaxbContext = JAXBContext.newInstance(OpenDRIVE.class);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		OpenDRIVE data = (OpenDRIVE) jaxbUnmarshaller.unmarshal(input);
		
		return data;
	}
	
	private void parse(InputStream input, ProgressMonitor progressMonitor) {
		try {
			OpenDRIVE data = readOpenDriveData(input);
			progressMonitor.setTicksCount(data.getLinkOrGeometryOrOutline().size());
			for (int i = 0; i< data.getLinkOrGeometryOrOutline().size(); i++) {
				Object item = data.getLinkOrGeometryOrOutline().get(i);
				progressMonitor.setTicks(i);
				Class<? extends Object> className = item.getClass();
				if (className==org.openstreetmap.josm.plugins.apolloopendrive.xml.OpenDRIVE.Road.class) {
					Road road = (Road)item;
					Relation roadRelation = createRelation("xodr:road");
					setRoadTags(roadRelation, "-1", road.getId(), road.getJunction()!= null ? road.getJunction() : "-1");
					road.getLink().forEach(link->{
						link.getNeighbor().forEach(neighbor->{
							addNeighborTags(roadRelation, neighbor.getId());
						});
						link.getPredecessor().forEach(predecessor->{
							addPredecessorTags(roadRelation, predecessor.getId());
						});
						link.getSuccessor().forEach(successor->{
							addSuccessorTags(roadRelation, successor.getId());
						});
					});
					road.getObjects().forEach(object->{
						object.getObject().forEach(singleObject->{
							singleObject.getOutline().forEach(outline->{
								Way way = createWay();
								setObjectTags(way, "-1", singleObject.getId(), singleObject.getType());
								roadRelation.addMember(new RelationMember("xodr:objectOutline", way));
								outline.getCornerGlobal().forEach(cornerGlobal ->{
									Node node = createNode(Double.parseDouble(cornerGlobal.getY()), Double.parseDouble(cornerGlobal.getX()), way);
								});
							});
							singleObject.getGeometry().forEach(geometry->{
								Way way = createWay();
								setObjectTags(way, "-1", singleObject.getId(), singleObject.getType());
								roadRelation.addMember(new RelationMember("xodr:objectGeometry", way));
								geometry.getPointSet().forEach(pointSet ->{
									pointSet.getPoint().forEach(point->{
										Node node = createNode(Double.parseDouble(point.getY()), Double.parseDouble(point.getX()), way);
									});
								});
							});
						});
					});
					road.getLanes().forEach(lane->{
						lane.getLaneSection().forEach(laneSection->{
							laneSection.getBoundaries().forEach(boundary->{
								boundary.getBoundary().forEach(boundary2->{
									boundary2.getGeometry().forEach(geometry->{
										geometry.getPointSet().forEach(pointSet->{
											Way way = createWay();
											setRoadBoundaryTags(way, boundary2.getType());
											roadRelation.addMember(new RelationMember("xodr:boundaryGeometry", way));
											pointSet.getPoint().forEach(point->{
												Node node = createNode(Double.parseDouble(point.getY()), Double.parseDouble(point.getX()), way);
											});
										});
									});
								});
							});
							laneSection.getCenter().forEach(center->{
								center.getLane().forEach(lane2->{
									lane2.getBorder().forEach(border->{
										border.getGeometry().forEach(geometry->{
											geometry.getPointSet().forEach(pointSet->{
												Way way = createWay();
												setCenterLaneTags(way, "-1", lane2.getId());
												roadRelation.addMember(new RelationMember("xodr:centerGeometry", way));
												pointSet.getPoint().forEach(point->{
													Node node = createNode(Double.parseDouble(point.getY()), Double.parseDouble(point.getX()), way);
												});
											});
										});
									});
								});
							});
							laneSection.getRight().forEach(right->{
								right.getLane().forEach(lane2->{
									lane2.getBorder().forEach(border->{
										border.getGeometry().forEach(geometry->{
											geometry.getPointSet().forEach(pointSet->{
												Way way = createWay();
												setLaneBorderTags(way, border.getVirtual());
												roadRelation.addMember(new RelationMember("xodr:rightBorderGeometry", way));
												pointSet.getPoint().forEach(point->{
													Node node = createNode(Double.parseDouble(point.getY()), Double.parseDouble(point.getX()), way);
												});
											});
										});
									});
									lane2.getCenterLine().forEach(center->{
										center.getGeometry().forEach(geometry->{
											geometry.getPointSet().forEach(pointSet->{
												Way way = createWay();
												setLaneTags(way, "-1", lane2.getId(), lane2.getType(), lane2.getDirection(), lane2.getTurnType());
												way.getKeys().putAll(getLaneLinks(lane2));
												roadRelation.addMember(new RelationMember("xodr:rightCenterGeometry", way));
												pointSet.getPoint().forEach(point->{
													Node node = createNode(Double.parseDouble(point.getY()), Double.parseDouble(point.getX()), way);
												});
											});
										});
									});
								});
							});
							laneSection.getLeft().forEach(left->{
								left.getLane().forEach(lane2->{
									lane2.getBorder().forEach(border->{
										border.getGeometry().forEach(geometry->{
											geometry.getPointSet().forEach(pointSet->{
												Way way = createWay();
												setLaneBorderTags(way, border.getVirtual());
												roadRelation.addMember(new RelationMember("xodr:leftBorderGeometry", way));
												pointSet.getPoint().forEach(point->{
													Node node = createNode(Double.parseDouble(point.getY()), Double.parseDouble(point.getX()), way);
												});
											});
										});
									});
									lane2.getCenterLine().forEach(center->{
										center.getGeometry().forEach(geometry->{
											geometry.getPointSet().forEach(pointSet->{
												Way way = createWay();
												setLaneTags(way, "-1", lane2.getId(), lane2.getType(), lane2.getDirection(), lane2.getTurnType());
												way.getKeys().putAll(getLaneLinks(lane2));
												roadRelation.addMember(new RelationMember("xodr:leftCenterGeometry", way));
												pointSet.getPoint().forEach(point->{
													Node node = createNode(Double.parseDouble(point.getY()), Double.parseDouble(point.getX()), way);
												});
											});
										});
									});
								});
							});
						});
					});
					road.getSignals().forEach(signals->{
						signals.getSignal().forEach(signal->{
							signal.getOutline().forEach(outline->{
								Way way = createWay();
								setSignalOutlineTags(way, "-1", signal.getId(), signal.getType(), signal.getLayoutType());
								roadRelation.addMember(new RelationMember("xodr:signalOutline", way));
								outline.getCornerGlobal().forEach(cornerGlobal ->{
									Node node = createNode(Double.parseDouble(cornerGlobal.getY()), Double.parseDouble(cornerGlobal.getX()), way);
								});
							});
							signal.getSubSignal().forEach(subSignal->{
								subSignal.getCenterPoint().forEach(centerPoint->{
									Node node = createNode(Double.parseDouble(centerPoint.getY()), Double.parseDouble(centerPoint.getX()));
									setSubSignalTags(node, "-1", subSignal.getId(), signal.getId(), subSignal.getType());
									roadRelation.addMember(new RelationMember("xodr:subsignal", node));
								});
							});
						});
					});
				}
			}
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Creates a new OSM node with the specified coordinates.
	 * @param lat Latitude of the node.
	 * @param lon Longitude of the node.
	 * @return The newly created node.
	 */
	private Node createNode(final double lat, final double lon) {
		final Node node = new Node(new LatLon(lat, lon));
		getDataSet().addPrimitive(node);
		System.out.println("new geometry: " + lat + " " + lon);
		return node;
	}
	
	/**
	 * Creates a new OSM node with the specified coordinates and adds it to the specified way.
	 * @param lat Latitude of the node.
	 * @param lon Longitude of the node.
	 * @param way Way the node to be added to.
	 * @return The newly created node.
	 */
	private Node createNode(final double lat, final double lon, Way way) {
		final Node node =  new Node(new LatLon(lat, lon));
		getDataSet().addPrimitive(node);
		way.addNode(node);
		return node;
	}
	
	/**
	 * Creates a new OSM relation of the specified type.
	 * @param type Type of the relation. This value is set as the "type" tag.
	 * @return The newly created relation.
	 */
	private Relation createRelation(String type) {
		final Relation relation = new Relation();
		Map<String, String> map = new HashMap<String, String>();
		map.put("type", type);
		relation.setKeys(map);
		getDataSet().addPrimitive(relation);
		return relation;
	}
	
	/**
	 * Creates a new OSM way.
	 * @return The newly created way.
	 */
	private Way createWay() {
		final Way way = new Way();
		getDataSet().addPrimitive(way);
		return way;
	}
	
	private void setObjectTags(OsmPrimitive primitive, String id, String uid, String type) {
		Map<String, String> map = new HashMap<String, String>();
		map.put("id", id);
		map.put("xodr:uid", uid);
		map.put("xodr:type", type);
		primitive.setKeys(map);
	}
	
	private void setSignalOutlineTags(OsmPrimitive primitive, String id, String uid, String type, String layoutType) {
		Map<String, String> map = new HashMap<String, String>();
		map.put("id", id);
		map.put("xodr:uid", uid);
		map.put("xodr:type", type);
		map.put("xodr:layoutType", layoutType);
		primitive.setKeys(map);
	}
	
	private void setSubSignalTags(OsmPrimitive primitive, String id, String uid, String singalUid, String type) {
		Map<String, String> map = new HashMap<String, String>();
		map.put("id", id);
		map.put("xodr:uid", uid);		
		map.put("xodr:signalUid", uid);
		map.put("xodr:type", type);
		primitive.setKeys(map);
	}
	
	private void setStopSignTags(OsmPrimitive primitive, String id, String uid, String referenceId) {
		Map<String, String> map = new HashMap<String, String>();
		map.put("id", id);
		map.put("xodr:signalUid", uid);
		map.put("xodr:referenceId", referenceId);
		primitive.setKeys(map);
	}
	
	private void setRoadTags(OsmPrimitive primitive, String id, String uid, String junction) {
		Map<String, String> map = new HashMap<String, String>();
		map.put("id", id);
		map.put("xodr:uid", uid);
		map.put("xodr:junction", junction);
		primitive.setKeys(map);
	}
	
	private void addNeighborTags(OsmPrimitive primitive, String id) {
		Map<String, String> map = new HashMap<String, String>();
		map.put("xodr:neighbor", id);
		primitive.getKeys().putAll(map);
	}
	
	private void addPredecessorTags(OsmPrimitive primitive, String id) {
		Map<String, String> map = new HashMap<String, String>();
		map.put("xodr:predecessor", id);
		primitive.getKeys().putAll(map);
	}
	
	private void addSuccessorTags(OsmPrimitive primitive, String id) {
		Map<String, String> map = new HashMap<String, String>();
		map.put("xodr:successor", id);
		primitive.getKeys().putAll(map);
	}
	
	private void setRoadBoundaryTags(OsmPrimitive primitive, String type) {
		Map<String, String> map = new HashMap<String, String>();
		map.put("xodr:type", type);
		primitive.setKeys(map);
	}
	
	private void setCenterLaneTags(OsmPrimitive primitive, String id, String uid) {
		Map<String, String> map = new HashMap<String, String>();
		map.put("id", id);
		map.put("xodr:uid", uid);
		primitive.setKeys(map);
	}
	
	private void setLaneTags(OsmPrimitive primitive, String id, String uid, String type, String direction, String turnType) {
		Map<String, String> map = new HashMap<String, String>();
		map.put("id", id);
		map.put("xodr:uid", uid);
		map.put("xodr:type", type);
		map.put("xodr:direction", direction);
		map.put("xodr:turnType", turnType);
		primitive.setKeys(map);
	}
	
	private void setLaneBorderTags(OsmPrimitive primitive, String virtual) {
		Map<String, String> map = new HashMap<String, String>();
		map.put("xodr:virtual", virtual);
		primitive.setKeys(map);
	}
	
	private Map<String, String> getLaneLinks(Lane lane){
		Map<String, String> map = new HashMap<String, String>();
		lane.getLink().forEach(link->{
			link.getNeighbor().forEach(neighbor->{
				map.put("xodr:neighbor", neighbor.getId());
			});
			link.getPredecessor().forEach(predecessor->{
				map.put("xodr:predecessor", predecessor.getId());
			});
			link.getSuccessor().forEach(successor->{
				map.put("xodr:successor", successor.getId());
			});
		});
		return map;
	}
	
	@Override
	protected DataSet doParseDataSet(InputStream input, ProgressMonitor progressMonitor) throws IllegalDataException {
		parse(input, progressMonitor);
		return getDataSet();
	}
	
	public static DataSet parseDataSet(InputStream input, ProgressMonitor progressMonitor) throws IllegalDataException {
		return new ApolloOpenDriveToOsmReader().doParseDataSet(input, progressMonitor);
	}

}
