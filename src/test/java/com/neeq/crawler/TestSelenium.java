package com.neeq.crawler;

import org.junit.Test;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Created by bj on 16/7/20.
 */
public class TestSelenium {
    @Test
    public void Test() {
        RemoteWebDriver driver = MyBrowserDriver.getChromeDriver();
        driver.switchTo();

    }
}
