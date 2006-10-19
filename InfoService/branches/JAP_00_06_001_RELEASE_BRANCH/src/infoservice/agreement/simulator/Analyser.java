package infoservice.agreement.simulator;

// package de.lg.da.agreement.test;
//
// import java.util.Enumeration;
// import java.util.Hashtable;
//
// import de.lg.da.agreement.orga.GoodInfoService;
// import de.lg.da.agreement.orga.InfoServiceImpl;
// import de.lg.da.agreement.orga.interfaces.IInfoService;
//
// public class Analyser implements LogHolder {
//
// // private Map<String, Map<String, Behaviour>> stats = new HashMap<String,
// Map<String, Behaviour>>();
// private Hashtable results = new Hashtable();
//    
// // public void report(String generel, String lieutenant, Behaviour behave) {
// // if(stats.get(generel) == null) {
// // stats.put(generel, new HashMap<String, Behaviour>());
// // }
// // if(stats.get(generel).containsKey(lieutenant)) {
// // throw new RuntimeException("Lieutenant " + lieutenant + " reports
// again!");
// // }
// // stats.get(generel).put(lieutenant, behave);
// // }
//
// public String getIdentifier() {
// return "Analyser";
// }
//
// public String getPrefix() {
// return "";
// }
//    
// public void analyse() {
// // for(String generel : stats.keySet()) {
// // Map<String, Behaviour> g = stats.get(generel);
// // boolean agreement = true;
// // Behaviour b = null;
// // for(String liu : g.keySet()) {
// // if(b == null) {
// // b = g.get(liu);
// // } else {
// // if(!g.get(liu).equals(b)) {
// // agreement = false;
// // }
// // }
// // }
// // if(agreement) {
// // Logger.getInstance().log(this, "AGREEMENT for generel: " + generel +
// ":\tHe is " + b.toString());
// // } else {
// // Logger.getInstance().log(this, "DISSENSE for generel: " + generel + "!");
// // }
// // }
// long result = 0;
// boolean goodAgreeVal = true;
// Enumeration en = this.results.keys();
// while(en.hasMoreElements()) {
// IInfoService general = (IInfoService) en.nextElement();
// if(general instanceof GoodInfoService) {
// if(result == 0) {
// result = ((Long)this.results.get(general)).longValue();
// } else {
// Long tmp = (Long)this.results.get(general);
// goodAgreeVal &= tmp.equals(Long.valueOf(result));
// }
// }
// // Logger.getInstance().log(this, general.getIdentifier() + " uses " +
// results.get(general));
// }
// if(goodAgreeVal) {
//            
// Logger.getInstance().log(this, "Good InfoServices agree to use " + result);
// } else {
// Logger.getInstance().log(this, "Good InfoServices DISAGREE!");
// }
//
// }
//
// public void overall(AInfoService name, Long overallResult) {
// if(this.results.get(name) != null) {
// throw new RuntimeException("Lieutenant " + name + " reports again!");
// }
// this.results.put(name, overallResult);
// }
//
// public void resetAll() {
// // stats.clear();
// this.results.clear();
// }
//
// }
