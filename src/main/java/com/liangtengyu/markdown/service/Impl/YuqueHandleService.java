package com.liangtengyu.markdown.service.Impl;

import com.liangtengyu.markdown.entity.MarkDown;
import com.liangtengyu.markdown.utils.MarkDownUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import static com.liangtengyu.markdown.utils.OSUtils.*;


@Slf4j
public class YuqueHandleService extends MarkDownService {


    @Override
    protected Document getHtmlContent(MarkDown markDown, Document document) {

        //启动chrome
        Element element = startChrome(markDown.getBlogUrl());

        // 去掉代码块中的行号
        try {
            Elements elements = element.getElementsByClass("cm-gutters");
            if (MarkDownUtil.elementsNotEmpty(elements)) {
                for (Element ele : elements) {
                    ele.remove();
                }
            }

            // 去掉返回文档按钮
            Elements elements2 = element.getElementsByClass("ne-viewer-header");
            if (MarkDownUtil.elementsNotEmpty(elements2)) {
                for (Element ele : elements2) {
                    ele.remove();
                }
            }


            Elements lines = element.getElementsByClass("cm-line");
            for (Element line : lines) {
                Element sp = new Element("span");
                sp.html(line.html());
                line.replaceWith(sp);
            }


            Elements codes = element.getElementsByClass("cm-content");
            for (Element code : codes) {
                Element pre = new Element("pre");
                pre.html(code.html());
                code.replaceWith(pre);
            }
        } catch (NullPointerException e) {
            log.info("一些为空");
        }
        //只要主要信息
        Elements elementsByClass = element.getElementsByClass("ne-viewer-body");
        String html = elementsByClass.html();

        return Jsoup.parse(html);
    }



    //"https://www.yuque.com/wangwangbunian-izczt/by3cic/vfw8xz6t003msaba#35c267ba"
    public static Element startChrome(String url) {
        String driverName = "chromeDriver/chromedriver-win64/chromedriver.exe";

        if (isWindows()) {
            log.info("当前操作系统为 Windows");
            driverName = "chromeDriver/chromedriver_win64/chromedriver.exe";

        } else if (isMac()) {
            log.info("当前操作系统为 macOS");
            if (isARM()) {
                log.info("ARM 架构");
                driverName = "chromeDriver/chromedriver_mac64/chromedriver";
            } else {
                log.info("非 ARM 架构");
                driverName = "chromeDriver/chromedriver_mac_arm64/chromedriver";
            }
        } else if (isLinux()) {
            log.info("当前操作系统为 Linux");
            driverName = "chromeDriver/chromedriver_linux64/chromedriver";
        } else {
            log.info("无法识别的操作系统");
        }

        System.setProperty("webdriver.chrome.driver", driverName);

        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--headless");
        chromeOptions.addArguments("--disable-gpu");
        chromeOptions.addArguments("--no-sandbox");
        WebDriver driver = new ChromeDriver(chromeOptions);
        // 使用try-with-resources确保WebDriver在代码块结束后关闭
        try {
            // 打开网页
            driver.get(url);

            // 使用JavascriptExecutor滚动到页面底部
            JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
            // 模拟拖动滚动条从1%到100%
            for (int i = 1; i <= 100; i++) {
                jsExecutor.executeScript("window.scrollTo(0," + i / 100.0 + "* document.body.scrollHeight );");
                Thread.sleep(50);
            }
            log.info("开始滚动代码块");

            // 使用WebDriverWait等待页面加载完成
            WebDriverWait wait = new WebDriverWait(driver, 10);


            WebElement reactApp1 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("ReactApp")));
            try {
                WebElement codeBlock = wait.until(ExpectedConditions.presenceOfElementLocated(By.className("cm-scroller")));
                jsExecutor.executeScript("arguments[0].scrollTop = 0;", codeBlock);
                // 等待一段时间，确保内容加载（你可以根据实际情况调整等待时间）
                Thread.sleep(1000);
                // 使用JavascriptExecutor再滚动到页面底部
                jsExecutor.executeScript("arguments[0].scrollTop = arguments[0].scrollHeight;", codeBlock);
            } catch (TimeoutException e) {
                log.info("滚动条等超时");
            }
            // 继续等待内部代码块组件的加载

            // 使用Jsoup解析页面
            String htmlCode = driver.getPageSource();
            Document parse = Jsoup.parse(htmlCode);

            return parse.getElementById("ReactApp").getElementById("content");

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            // 关闭浏览器
            driver.quit();
        }
    }
}
