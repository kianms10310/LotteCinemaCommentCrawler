

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LotteMovieCommentCrawler {

private static final Logger Logger = LoggerFactory.getLogger(LotteMovieCommentCrawler.class);
	
	private String chromedriverpath="./libnative/webdrivers/chromedriver-2.44-windows-x86-32.exe";		//디폴트 크롬헤드리스 드라이버위치
	//private String chromedriverpath="./libnative/webdrivers/chromedriver_linux64";		//디폴트 크롬헤드리스 드라이버위치
	
	private String phantomjs_driver_path="./libnative/webdrivers/phantomjs-2.1.1-windows-x86-32.exe";		//디폴트 크롬헤드리스 드라이버위치
	//private String phantomjs_driver_path="./libnative/webdrivers/phantomjs-2.1.1-linux-x86_64";		//디폴트 크롬헤드리스 드라이버위치

	public WebDriver driver=null;

	public LotteMovieCommentCrawler() {
	}	
	
	//크롬브라우져 로드함.
	public void setup_chrome(boolean headless) {
		System.setProperty("webdriver.chrome.driver", this.chromedriverpath);
		
		if(headless) {
			//크롬브라우져를 시뮬레이션함
			final org.openqa.selenium.chrome.ChromeOptions options = new org.openqa.selenium.chrome.ChromeOptions();
			options.addArguments("headless");	//브라우져를 띄우지 않음
			options.addArguments("window-size=1920x1080");	//요즘 해상도 기준임
			options.addArguments("disable-gpu"); // 관련라이브러리 에러가 발생함 gpu옵션 제거
			options.addArguments("--no-sandbox");
			 
			options.addArguments("user-agent=Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.80 Safari/537.36");
			//co.addArguments("lang=ko_KR");	// # 한국어!
			
			//아래 옵션을 추가하니 driver.get(url)에서 한참을 대기하지 않고 즉시 결과가 나옴
			options.addArguments("--proxy-server='direct://'");
			options.addArguments("--proxy-bypass-list=*");			
			
			//크롬 브라우져를 열고 http://127.0.0.1:9222로 접속하면, 디버깅도구를 사용할 수 있다.
			//로컬만 접속이 가능하니, 리눅스(원격)에서 실행 했을 경우는 SSH의 포트 포워딩을 이용하자.
			//https://blog.outsider.ne.kr/1291
			options.addArguments("remote-debugging-port=9222");
			 
			 this.driver = new org.openqa.selenium.chrome.ChromeDriver(options);
		} else {
			//실제 크롬브라우져가 실행됨.
			this.driver = new org.openqa.selenium.chrome.ChromeDriver();
		}
	}
	
	public void setup_phantomjs(boolean headless) {
		System.setProperty("phantomjs.binary.path",	this.phantomjs_driver_path);

		final org.openqa.selenium.remote.DesiredCapabilities caps = new org.openqa.selenium.remote.DesiredCapabilities();
	    caps.setJavascriptEnabled(true); // not really needed: JS enabled by default
	    caps.setCapability(org.openqa.selenium.phantomjs.PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, this.phantomjs_driver_path);
		
	    this.driver= new org.openqa.selenium.phantomjs.PhantomJSDriver(caps);
	    //driver.manage().window().maximize();
	    //driver.navigate().to("https://www.google.co.in/");		
	}	
	
	//드라이버 끈내기, 관련된 모든 윈도우를 닫는다.
	public void shutdownNow() {
		if ( this.driver==null ) return;
		
		this.driver.quit();
		this.driver=null;
	}
	
	private void killChromedriver() throws IOException, InterruptedException {
	    	String command = "pgrep chromedriver";
	    	Process process = Runtime.getRuntime().exec(command);
	    	BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
	    	List<String> processIds = getProcessedIds (process, br);
	    	for (String pid: processIds) {
	    		Process p = Runtime.getRuntime().exec("kill -9 " + pid);
	            p.waitFor();
	            p.destroy();
	    	}
	}
	 
    private List<String> getProcessedIds(Process process, BufferedReader br) throws IOException, InterruptedException {
        process.waitFor();

        List<String> result = new ArrayList<String>();
        String processId ;

        while (null != (processId = br.readLine())) {
            result.add(processId);
        }

        process.destroy();
        return result;
    }
    
    public boolean get(final String url, final int timeout_second) throws InterruptedException {
    	//주소 참조: https://beomi.github.io/2017/10/29/HowToMakeWebCrawler-ImplicitWait-vs-ExplicitWait/
    	//this.driver.manage().timeouts().implicitlyWait(timeout_second, TimeUnit.SECONDS);
    	
    	//페이지 로딩시에 타임아웃 설정    
        this.driver.manage().timeouts().pageLoadTimeout(timeout_second, TimeUnit.SECONDS);
        //HTML요소가 완료되지 않았을 때, findElement 값을 읽으면 완성될때 까지 기다릴 타임아웃 설정    
        //this.driver.manage().timeouts().implicitlyWait(timeout_second, TimeUnit.SECONDS);
        //스크립트 타임아웃 설정    
        this.driver.manage().timeouts().setScriptTimeout(timeout_second, TimeUnit.SECONDS);
        
		this.driver.get( url );
		
		return true;
    }
	
	public static void main(String[] args) {
			final String url=(args.length<=0) ? "http://www.lottecinema.co.kr/LCHS/Contents/Movie/Movie-Detail-View.aspx?movie=14151" : args[0];
			//final String url="http://www.naver.com";
			List<WebElement> list = null;
			LotteMovieCommentCrawler crawler=new LotteMovieCommentCrawler();
			List<String> co = new ArrayList<>();
			
			FileWriter fw = null;
			
			try {
				final Scanner scanner = new Scanner(System.in);

				crawler.setup_chrome(false);
				//crawler.setup_phantomjs(true);
				crawler.get(url, 300);
				Thread.sleep(1000*5); //잠시 대기를 해야 AJAX 페이지들이 완성된다.
				
				WebDriver driver = crawler.driver;
				driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);

				while(true) {
					List<WebElement> comments = driver.findElements(By.xpath("//div[@class='score_result']/ul[@id='ulReviews']/li"));
					
					for(int idx = 0; idx < comments.size(); idx++) {
						WebElement scoreElement = comments.get(idx).findElements(By.xpath("//div[@class='score_box']/div[@class='score_sum']/span[@class='score_txt']")).get(idx);
						WebElement txtElement = comments.get(idx).findElements(By.xpath("//div[@class='score_box']/p[@class='result_txt']")).get(idx);
						WebElement dateElement = comments.get(idx).findElements(By.xpath("//div[@class='score_box']/div[@class='score_clicks']/span[@class='score_date']")).get(idx);
						
						String score = scoreElement.getText();
						String txt = txtElement.getText();
						String date = dateElement.getText();
						String str = txt + "\t" + score + "\t" + date +"\r\n";
						co.add(str);
						System.out.print(str);
					}
					
					List<WebElement> pages = driver.findElements(By.xpath("//div[@class='review_wrap']/div[@class='paging']/span[@class='pagingNum']/a"));
					WebElement page = driver.findElement(By.xpath("//div[@class='review_wrap']/div[@class='paging']/span[@class='pagingNum']/a[@class='on']"));
					Integer pageNum = Integer.parseInt(page.getText());
					if(pages.size() > pageNum + 1) {
						pages.get(pageNum).click();
					}else {
						break;
					}
					Thread.sleep(1000*5);
				}
				
				
			} catch (Throwable e) {
				e.printStackTrace();
			} finally {
				if ( crawler!=null ) crawler.shutdownNow();
			};
			
			try {
				fw = new FileWriter(new File("./MovieComment/Lotte.txt"));
				for(String s : co ) {
					fw.write(s);
				}
			}catch(Exception e) {
				
			}finally {
				if(fw != null) {
					try {
						fw.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
	}

}
