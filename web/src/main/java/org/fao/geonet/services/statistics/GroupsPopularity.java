package org.fao.geonet.services.statistics;

import java.awt.Color;
import java.io.File;
import java.util.List;

import jeeves.constants.Jeeves;
import jeeves.resources.dbms.Dbms;
import jeeves.server.ServiceConfig;
import jeeves.server.context.ServiceContext;
import jeeves.utils.IO;
import jeeves.utils.Log;

import org.fao.geonet.GeonetContext;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.services.NotInReadOnlyModeService;
import org.jdom.Element;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;

/**
 * Service to get the db-stored requests information group by popularity
 * todo: factorize chart code into a factory...
 * todo I18N all strings
 * @author nicolas Ribot
 *
 */
public class GroupsPopularity extends NotInReadOnlyModeService{
	/** the SQL query to get results */
	private String query;
	/** should we generate and send tooltips to client (caution, can slow down the process if
	 * dataset is big)
	 */
	private boolean createTooltips;
	
	/** the imagemap for this chart, allowing to display tooltips */
	private String imageMap;
	/** should we generate and send legend to client (caution, can slow down the process if
	 * dataset is big)
	 */
//	private boolean createLegend;
	
	/** chart width, service parameter, can be overloaded by request */
	private int chartWidth;
	
	/** chart width, can be overloaded by request */
	private int chartHeight;
	
	//--------------------------------------------------------------------------
	//---
	//--- Init
	//---
	//--------------------------------------------------------------------------

	public void init(String appPath, ServiceConfig params) throws Exception	{
        super.init(appPath, params);
//		this.createLegend = Boolean.parseBoolean(params.getValue("createLegend"));
		this.createTooltips = Boolean.parseBoolean(params.getValue("createTooltips"));
		this.chartWidth = Integer.parseInt(params.getValue("chartWidth"));
		this.chartHeight = Integer.parseInt(params.getValue("chartHeight"));
		this.query = params.getValue("query");
	}

	//--------------------------------------------------------------------------
	//---
	//--- Service
	//---
	//--------------------------------------------------------------------------
    @Override
	public Element serviceSpecificExec(Element params, ServiceContext context) throws Exception {
        String message = "";
		GeonetContext gc = (GeonetContext) context.getHandlerContext(Geonet.CONTEXT_NAME);
		
		// gets the total popularity count (=100)
		Dbms dbms = (Dbms) context.getResourceManager().open(Geonet.Res.MAIN_DB);
		
		// wont work if there is no metadata
		@SuppressWarnings("unchecked")
        List<Element> l  = dbms.select("select sum(popularity) as sumpop from metadata").getChildren();
		if (l.size() != 1) {
			message = "cannot get popularity count";
			return null;
		}
		
		int cnt = Integer.parseInt(l.get(0).getChildText("sumpop"));

        if(Log.isDebugEnabled(Geonet.SEARCH_LOGGER))
            Log.debug(Geonet.SEARCH_LOGGER,"query to get popularity by group:\n" + query);
		dbms = (Dbms) context.getResourceManager().open(Geonet.Res.MAIN_DB);
		
		DefaultPieDataset dataset = new DefaultPieDataset(); 
		@SuppressWarnings("unchecked")
        List<Element> resultSet = dbms.select(query).getChildren();
		
		for (Element record : resultSet) {
			String popularity = (record).getChildText("popularity");
			Double d = 0.0;
			if (popularity.length() > 0 ) {
				d = (Double.parseDouble(popularity) / cnt ) * 100; 
			}
			dataset.setValue(record.getChildText("groupname"),d);
			//System.out.println(record.getChildText("groupname") + ", " + d);
		}
		
		// create a chart... 
		JFreeChart chart = ChartFactory.createPieChart( 
			null, 
			dataset, 
			true, // legend? 
			true, // tooltips? 
			false // URLs? 
		); 


		//hard coded values for the moment. should come from a configuration file.
		chart.setBackgroundPaint(Color.decode("#E7EDF5"));
		String chartFilename = "popubygroup_" + System.currentTimeMillis() + ".png";
		
		File statFolder = new File(gc.getHandlerConfig().getMandatoryValue(
				Geonet.Config.RESOURCES_DIR) + File.separator + "images" + File.separator + "statTmp");
		IO.mkdirs(statFolder, "Statistics temp directory");
		File f = new File(statFolder, chartFilename);
		this.imageMap = org.fao.geonet.services.statistics.ChartFactory.writeChartImage(
				chart, f, this.chartWidth, this.chartHeight, this.createTooltips, "graphPopuByGroupImageMap");
		// will return some info to the XSLT:
		// dateFrom, dateTo, graphicType, chartUrl, tooltipImageMap,
		// message, chartWidth, chartHeight
		
		Element elResp = new Element(Jeeves.Elem.RESPONSE);
		Element elchartUrl = new Element("popuByGroupUrl").setText(context.getBaseUrl() + 
				"/images/statTmp/" + chartFilename);		
		Element elTooltipImageMap = new Element("tooltipImageMap").addContent(
				this.createTooltips ? this.imageMap : "");
		
		Element elMessage = new Element("message").setText(message);		
		Element elChartWidth= new Element("chartWidth").setText("" + this.chartWidth);		
		Element elChartHeight= new Element("chartHeight").setText("" + this.chartHeight);	
		
		elResp.addContent(elchartUrl);
		elResp.addContent(elTooltipImageMap);
		elResp.addContent(elMessage);
		elResp.addContent(elChartWidth);
		elResp.addContent(elChartHeight);

		return elResp;
	}
}
