//Released under the terms of the GNU GPL v2
//SeleNesse is maintained by Marisa Seal and Chris McMahon
//Portions of SeleNesse based on code originally written by Gojko Adzic http://gojko.net
package selenesse;

import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;
import com.thoughtworks.selenium.SeleniumException;
import com.thoughtworks.selenium.Wait;
import fitnesse.slim.SystemUnderTest;

import java.net.MalformedURLException;
import java.net.URL;

public class SlimSeleniumDriver {

    private static final String KNOWN_SELENIUM_BUG_EXCEPTION_MESSAGE = "Couldn't access document.body";
    private static final String FORWARD_SLASH = "/";
    private String timeoutSeconds = "30";
    private String timeoutMilliseconds = timeoutSeconds + "000";
    @SystemUnderTest
    public Selenium seleniumInstance;

    public SlimSeleniumDriver(String host, int port, String browser, String baseURL) {
        seleniumInstance = new DefaultSelenium(host, port, browser, baseURL);
        seleniumInstance.start();
    }

    public String getCookies() {
        return seleniumInstance.getCookie();
    }

    //HTTP Requests
    /**
     * Makes a GET request to the given path using the cookies currently set on
     * the {@link Selenium} instance.
     *
     * @param path
     * @return the response
     */
    public String get(String path) throws Exception {
        return makeRequest("GET", path);
    }

    /**
     * Makes a PUT request to the given path using the cookies currently set on
     * the {@link Selenium} instance.
     *
     * @param path
     * @return the response
     */
    public String put(String path) throws Exception {
        return makeRequest("PUT", path);
    }

    /**
     * Makes a DELETE request to the given path using the cookies currently set
     * on the
     * {@link Selenium} instance.
     *
     * @param path
     * @return the response
     */
    public String delete(String path) throws Exception {
        return makeRequest("DELETE", path);
    }

    /**
     * Makes a simple file POST request to the given path using the cookies
     * currently set on the
     * {@link Selenium} instance.
     *
     * @param path
     * @param mediaType
     * @param filename
     * @return the response
     */
    public String postFile(String path, String mediaType, String filename) throws Exception {
        String url = getFormattedURL(getBaseURL(), path);
        return HttpUtils.postSimpleFile(url, getCookies(), mediaType, filename);
    }

    // Makes a simple http request with the given request method.
    private String makeRequest(String requestMethod, String path) throws Exception {
        String url = getFormattedURL(getBaseURL(), path);
        return HttpUtils.makeRequest(requestMethod, url, getCookies());
    }

    public String getBaseURL() throws MalformedURLException {
        URL url = new URL(seleniumInstance.getLocation());
        return url.getProtocol() + "://" + url.getHost();
    }

    private String getFormattedURL(String baseURL, String path) {
        if (!path.startsWith(FORWARD_SLASH)) {
            path = FORWARD_SLASH + path;
        }
        return baseURL + path;
    }

    //Convenience methods
    public void setTimeoutSeconds(String seconds) {
        timeoutSeconds = seconds;
        timeoutMilliseconds = timeoutSeconds + "000";
        seleniumInstance.setTimeout(timeoutMilliseconds);
    }

    public void pause(int milliseconds) throws InterruptedException {
        Thread.sleep(milliseconds);
    }

    //Element interaction methods
    public void click(String locator) {
        seleniumInstance.click(locator);
    }

    public void clickAt(String locator, String coordinates) {
        seleniumInstance.clickAt(locator, coordinates);
    }

    public void clickUpToTimes(String locator, int numberOfTimesToExecute) {
        int tries = 0;
        while (seleniumInstance.isElementPresent(locator) && tries <= numberOfTimesToExecute) {
            try {
                seleniumInstance.click(locator);
            } catch (SeleniumException e) {
                if (!e.getMessage().contains("not found")) {
                    throw e;
                }

            }
            tries++;
        }
    }

    public void focus(String locator) {
        seleniumInstance.focus(locator);
    }

    public void makeChecked(String locator) {
        seleniumInstance.check(locator);
    }

    public void makeNotChecked(String locator) {
        seleniumInstance.uncheck(locator);
    }

    public void select(String selectLocator, String optionLocator) {
        if (!isOptionAlreadySelected(selectLocator, optionLocator)) {
            seleniumInstance.select(selectLocator, optionLocator);
        }
    }

    public void type(String locator, String text) {
        seleniumInstance.type(locator, text);
    }

    //_AndWait methods
    public void clickAndWait(String locator) {
        seleniumInstance.click(locator);
        seleniumInstance.waitForPageToLoad(timeoutMilliseconds);
    }

    public void selectAndWait(String selectLocator, String optionLocator) {
        if (!isOptionAlreadySelected(selectLocator, optionLocator)) {
            seleniumInstance.select(selectLocator, optionLocator);
            seleniumInstance.waitForPageToLoad(timeoutMilliseconds);
        }
    }

    //waitFor_ methods
    public void waitForEditable(String locator) {
        Wait w = new WaitForElementToBeEditable(locator);
        try {
            w.wait("Element " + locator + " not editable after " + timeoutSeconds + " seconds", Long.parseLong(timeoutMilliseconds));
        } catch (SeleniumException e) {
            if (isKnownSeleniumBug(e)) {
                waitForEditable(locator);
            } else {
                throw e;
            }
        }
    }

    public void waitForElementPresent(String locator) {
        Wait w = new WaitForElementToAppear(locator);
        try {
            w.wait("Cannot find element " + locator + " after " + timeoutSeconds + " seconds", Long.parseLong(timeoutMilliseconds));
        } catch (SeleniumException e) {
            if (isKnownSeleniumBug(e)) {
                waitForElementPresent(locator);
            } else {
                throw e;
            }
        }
    }

    public void waitForElementNotPresent(String locator) {
        Wait w = new WaitForElementToDisappear(locator);
        try {
            w.wait("Element " + locator + " still present after " + timeoutSeconds + " seconds", Long.parseLong(timeoutMilliseconds));
        } catch (SeleniumException e) {
            if (isKnownSeleniumBug(e)) {
                waitForElementNotPresent(locator);
            } else {
                throw e;
            }
        }
    }

    public void waitForVisible(String locator) {
        Wait x = new WaitForElementToBeVisible(locator);
        try {
            x.wait("Element " + locator + " not visible after " + timeoutSeconds + " seconds", Long.parseLong(timeoutMilliseconds));
        } catch (SeleniumException e) {
            if (isKnownSeleniumBug(e)) {
                waitForVisible(locator);
            } else {
                throw e;
            }
        }
    }

    public void waitForNotVisible(String locator) {
        Wait x = new WaitForElementToBeInvisible(locator);
        try {
            x.wait("Element " + locator + " still visible after " + timeoutSeconds + " seconds", Long.parseLong(timeoutMilliseconds));
        } catch (SeleniumException e) {
            if (isKnownSeleniumBug(e)) {
                waitForNotVisible(locator);
            } else {
                throw e;
            }
        }
    }

    public void waitForTextPresent(String text) {
        Wait x = new WaitForTextToAppear(text);
        try {
            x.wait("Cannot find text " + text + " after " + timeoutSeconds + " seconds", Long.parseLong(timeoutMilliseconds));
        } catch (SeleniumException e) {
            if (isKnownSeleniumBug(e)) {
                waitForTextPresent(text);
            } else {
                throw e;
            }
        }
    }

    public void waitForTextNotPresent(String text) {
        Wait x = new WaitForTextToDisappear(text);
        try {
            x.wait("Text " + text + " still present after " + timeoutSeconds + " seconds", Long.parseLong(timeoutMilliseconds));
        } catch (SeleniumException e) {
            if (isKnownSeleniumBug(e)) {
                waitForTextNotPresent(text);
            } else {
                throw e;
            }
        }
    }

    public void waitForSelectedLabel(String selectLocator, String label) {
        Wait x = new WaitForLabelToBeSelected(selectLocator, label);

        try {
            x.wait("Option with label " + label + " not selected in " + selectLocator + " after " + timeoutSeconds
                    + " seconds", Long.parseLong(timeoutMilliseconds));
        } catch (SeleniumException e) {
            if (isKnownSeleniumBug(e)) {
                waitForSelectedLabel(selectLocator, label);
            } else {
                throw e;
            }
        }
    }

    //Waiter classes
    protected class WaitForElementToBeEditable extends Wait {

        protected String locator;

        public WaitForElementToBeEditable(String locator) {
            this.locator = locator;
        }

        public boolean until() {
            return seleniumInstance.isEditable(locator);
        }
    }

    protected class WaitForElementToAppear extends Wait {

        protected String locator;

        public WaitForElementToAppear(String locator) {
            this.locator = locator;
        }

        public boolean until() {
            return seleniumInstance.isElementPresent(locator);
        }
    }

    protected class WaitForElementToBeVisible extends Wait {

        protected String locator;

        public WaitForElementToBeVisible(String locator) {
            this.locator = locator;
        }

        public boolean until() {
            return (seleniumInstance.isElementPresent(locator)
                    && seleniumInstance.isVisible(locator));
        }
    }

    protected class WaitForElementToBeInvisible extends Wait {

        protected String locator;

        public WaitForElementToBeInvisible(String locator) {
            this.locator = locator;
        }

        public boolean until() {
            return !seleniumInstance.isVisible(locator);
        }
    }

    protected class WaitForElementToDisappear extends Wait {

        protected String locator;

        public WaitForElementToDisappear(String locator) {
            this.locator = locator;
        }

        public boolean until() {
            return !seleniumInstance.isElementPresent(locator);
        }
    }

    protected class WaitForTextToAppear extends Wait {

        protected String text;

        public WaitForTextToAppear(String text) {
            this.text = text;
        }

        public boolean until() {
            return seleniumInstance.isTextPresent(text);
        }
    }

    protected class WaitForTextToDisappear extends Wait {

        protected String text;

        public WaitForTextToDisappear(String text) {
            this.text = text;
        }

        public boolean until() {
            return !seleniumInstance.isTextPresent(text);
        }
    }

    protected class WaitForLabelToBeSelected extends Wait {

        protected String locator;
        protected String label;

        public WaitForLabelToBeSelected(String locator, String label) {
            this.locator = locator;
            this.label = label;
        }

        public boolean until() {
            return seleniumInstance.getSelectedLabel(locator).equals(label);
        }
    }

    private boolean isOptionAlreadySelected(String selectLocator, String optionLocator) {
        return (seleniumInstance.isSomethingSelected(selectLocator))
                && isSelectSameAsOption(selectLocator, optionLocator);
    }

    private boolean isSelectSameAsOption(String selectLocator, String optionLocator) {
        return (isEqualLessPrefix(selectLocator, optionLocator, "id=")
                || isEqualLessPrefix(selectLocator, optionLocator, "label=")
                || isEqualLessPrefix(selectLocator, optionLocator, "value=")
                || isEqualLessPrefix(selectLocator, optionLocator, "index=")
                || (seleniumInstance.getSelectedLabel(selectLocator).equals(optionLocator)));
    }

    private boolean isEqualLessPrefix(String selectLocator, String optionLocator, String prefix) {
        return (optionLocator.startsWith(prefix)
                && seleniumInstance.getSelectedId(selectLocator).equals(optionLocator.replace(prefix, "")));
    }

    private boolean isKnownSeleniumBug(SeleniumException exception) {
        return exception.getMessage().contains(KNOWN_SELENIUM_BUG_EXCEPTION_MESSAGE);
    }
}
