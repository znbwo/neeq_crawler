package com.neeq.crawler;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.opera.OperaDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;

import java.util.concurrent.TimeUnit;

/**
 * Created by bj on 16/7/13.
 */
public class MyBrowserDriver {
    private static void init(RemoteWebDriver driver) {
        driver.manage().timeouts().pageLoadTimeout(5, TimeUnit.SECONDS);
        driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
        driver.manage().timeouts().setScriptTimeout(5, TimeUnit.SECONDS);

//        driver.manage().window().maximize();
    }

    public static RemoteWebDriver getChromeDriver() {
        System.getProperties().setProperty("webdriver.chrome.driver", "/Users/bj/Downloads/chromedriver");
        ChromeDriver driver = new ChromeDriver();
        init(driver);
        return driver;
    }

    public static RemoteWebDriver getOperaDriver() {
        System.getProperties().setProperty("webdriver.chrome.driver", "/Users/bj/Downloads/operadriver");
        OperaDriver driver = new OperaDriver();
        init(driver);
        return driver;
    }

    public static RemoteWebDriver getSafariDriver() {
        SafariDriver driver = new SafariDriver();
        init(driver);
        return driver;
    }

    public static RemoteWebDriver getFirefoxDriver() {
        FirefoxDriver driver = new FirefoxDriver();
        init(driver);
        return driver;
    }

    public static void repairByBrower(RemoteWebDriver driver) {
//        driver.manage().timeouts().pageLoadTimeout(10, TimeUnit.SECONDS);
        driver.get("http://tianyancha.com");
        WebElement input = driver.findElementById("live-search");
        input.sendKeys("中科软科技股份有限公司");
        WebElement submit = driver.findElement(By.cssSelector("div.input-group-addon.search_button"));
        submit.click();
        driver.get("http://tianyancha.com/search/中科软科技股份有限公司");

    }

    /**
     * 驱动安装在浏览器中
     *
     * @param url
     */
    public static void repairBySafari(String url) {
        RemoteWebDriver driver = getSafariDriver();
        driver.get(url);
    }
}
