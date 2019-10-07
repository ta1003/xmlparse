package com.meta.parse;


import java.io.IOException;


import javax.xml.parsers.ParserConfigurationException;


import org.xml.sax.SAXException;

public class XMLParser {

	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException{
		
		// 메모리 초기화
		System.gc();
		
		// 실행전 메모리 사용량 조회
		long preUseMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		
		long start = System.currentTimeMillis();	
		
		XMLParserProcess process = new XMLParserProcess("T_BASEFILE_TB.xml");
		process.process();		  
		
		long end = System.currentTimeMillis();
		
		System.out.println( "실행 시간 : " + (end-start)/1000.0 +"초");		
		
		// 메모리 정리
		System.gc();
		
		// 실행후 메모리 사용량 조회
		long afterUserMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		
		// 메모리 사용량 확인
		long useMemory = (preUseMemory - afterUserMemory)/1024;
		System.out.println("메모리 사용량 :"+useMemory+"Kbyte");
	}
}
