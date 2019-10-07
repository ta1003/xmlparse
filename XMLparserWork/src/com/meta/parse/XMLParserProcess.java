package com.meta.parse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLParserProcess {
	
	// 처음 불러올 파일의 이름
	private String initFilename;	
		
	public XMLParserProcess(String initFilename) {
		init(initFilename);
	}
	
	private void init(String initFilename) {
		this.initFilename = initFilename;
	}
	
	public void process() {		
		
		OutputStream updateFile = null;
		
		try {		 
			
		  // 파일 경로
		  String filePath = "files/";
			
		  // Doc 문서 작성
		  Document doc =  makeDoc(filePath+initFilename);		  
		  doc.getDocumentElement().normalize();
		  
		  // File id 추출		  
		  // Key : F_0_TB.xml   value : P_0_TB.xml
		  HashMap<String,String> fileList = getFileList("FILE_ID",doc);		  
		 
		  
		  // SIMILAR_RATE / 100 값이 50보다 큰  ROWID와 PID 를 파일 이름에 맞춰서 저장
		  // key : F_0_TB.xml   value : ROWID(key) , PID(value)
		  HashMap<String,HashMap<String,String>> resultRateMap = new HashMap<String,HashMap<String,String>>();
		  for(String filename : fileList.keySet()) {
			  HashMap<String,String> rowMatchPID = getRowAndPID(filePath+filename);			 
			  
			  // 조건에 맞는 값만 Map에 담음			  
			  if(rowMatchPID.size() > 0) {				  				 
				  resultRateMap.put(filename, rowMatchPID);
			  }
		  }		 
		  
		  // resultRateMap의 PID에 맞는 라이센스 ID를 가져옴
		  // key : PID  value : license
		 HashMap<String,String> licenseList = new HashMap<String,String>();		  
		  
		  for(String filename : resultRateMap.keySet()) {			 
			  Document p_doc =  makeDoc(filePath+fileList.get(filename));		  
			  p_doc.getDocumentElement().normalize();		 
			  
			  for(String rowID : resultRateMap.get(filename).keySet()) {
				  String pID = resultRateMap.get(filename).get(rowID);				  
				  String licenseID = getLicenseID(p_doc,pID);				  
				  if(pID != null && pID != "") {
					  licenseList.put(pID, licenseID);					 
				  }				 				 
			  }			  
		  }  	  
		  
		  
		  TransformerFactory transfactory = TransformerFactory.newInstance();
		  Transformer trans = transfactory.newTransformer();
		  trans.setOutputProperty(OutputKeys.METHOD, "xml");
		  trans.setOutputProperty(OutputKeys.ENCODING, "UTF-8");		  
		  
		// 조건에 맞는 문서를 가져와 수정
		  for(String filename : resultRateMap.keySet()) {
			  Document updateDoc =  makeDoc(filePath+filename);					 	 
			  NodeList updateList = updateDoc.getElementsByTagName("ROWID");
			  boolean updateChk = false;
			  for(int i=0; i < updateList.getLength(); i++) {
				  Element eRowid = (Element)updateList.item(i);				  
				  String pid = resultRateMap.get(filename).get(eRowid.getTextContent());				  
				  if(pid != "" && pid != null) {
					  String license = licenseList.get(pid);					  
					  NodeList commentList = updateDoc.getElementsByTagName("COMMENT");
					  Element eComment = (Element)commentList.item(i);
					  eComment.setTextContent(license);
					  System.out.println("filename:"+filename+"   rowid:"+eRowid.getTextContent()+"    pid:"+pid+"    comment:"+license);					  
					  updateDoc.getElementsByTagName("ROW").item(i).replaceChild(eComment, commentList.item(i));
					  updateChk = true;
				  }		  				  
			  }
			  
			  if(updateChk) {
				  DOMSource source = new DOMSource(updateDoc);			  
				  updateFile = new FileOutputStream("result/"+filename);			  
				  trans.transform(source, new StreamResult(updateFile));  
			  }
		  }		 		
		}catch(Exception e)	{									}
		finally 			{	closeFile(updateFile);			}
	}	
	// 문서 작성 메소드
	private Document makeDoc(String fileName) throws Exception{		
		  File fXmlFile = new File(fileName);
		  DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		  DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		  Document doc =  dBuilder.parse(fXmlFile);		  
		  return doc;		
	}
	
	private HashMap<String,String> getFileList(String file_id,Document doc) {
		  // 파일 목록을 관리하기 위한 Map
		  HashMap<String,String> fileList = new HashMap<String,String>();
		  
		  NodeList FileList = doc.getElementsByTagName(file_id);		 
		  
		  for(int i=0; i < FileList.getLength(); i++) {
		  Node fileNode =  FileList.item(i);
		  Element efile = (Element)fileNode;		  
		  StringBuffer filename = new StringBuffer();			  
		  filename.append(efile.getTextContent()).append("_TB.xml");		 
		  fileList.put("F_"+filename.toString(), "P_"+filename.toString());		
		}		  
		  return fileList;
	}
	
	// 해당 문서의 Row_id 와 pid를 매치
	private HashMap<String,String> getRowAndPID(String file_id) throws Exception{
		
		 
		 Document doc = makeDoc(file_id);
		 doc.getDocumentElement().normalize();
		 
		 HashMap<String,String> rowMatchPID = checkRate(doc);
		 
		 return rowMatchPID;
	}
	
	//rate가 조건에 맞는경우 ROWID,P_ID를 HashMap에 담음
	private HashMap<String,String> checkRate(Document doc) {		
		
		HashMap<String,String> resultMap = new HashMap<String,String>();
				
		NodeList RateList = doc.getElementsByTagName("SIMILAR_RATE");
		
		for(int i=0; i < RateList.getLength(); i++) {
			Node rateNode = RateList.item(i);
			Element eRate = (Element)rateNode;
			
			String rateValue = eRate.getTextContent();			
			int rateintVal = Integer.parseInt(rateValue);
			
			
			if(rateintVal/100 > 50) {
				
				Node rowID = doc.getElementsByTagName("ROWID").item(i);
				Node pID = doc.getElementsByTagName("P_ID").item(i);				
				resultMap.put(((Element)rowID).getTextContent(), ((Element)pID).getTextContent());			
				
			}else{
				
			}
		}
		
		return resultMap;		
	}
	
	
	  //PID에 맞는 라이센스ID를 가져옴 
	private String getLicenseID(Document doc,String pid) {		
			
			String licenseID = null;
					
			NodeList pidList = doc.getElementsByTagName("P_ID");
			
			for(int i=0; i < pidList.getLength(); i++) {
				Node pidNode = pidList.item(i);
				Element ePid = (Element)pidNode;				
				String ePidValue = ePid.getTextContent();
				
				if(pid.equals(ePidValue)) {
					NodeList licenseList = doc.getElementsByTagName("LICENSE_ID");
					Node licenseNode = licenseList.item(i);
					Element eLicense =  (Element)licenseNode;
					licenseID = eLicense.getTextContent();
					if(licenseID == null) { licenseID =""; }
				}							
			}
			
			return licenseID;		
		}		
		
		private void closeFile(OutputStream file) {
		    
		        if (file != null) {
		            try {
		            	file.close();
		            } catch (IOException e) {
		                e.printStackTrace();
		            }
		        }
		    
		}	
}
