package splish

import scopt._
import requests._
import requests.RequestAuth
import requests.RequestAuth.Basic
import requests.Response
import ujson.Js

/**
 * A class to facilitate access to a Splash instance.
 *
 * Splash is a javascript rendering service. It’s a lightweight web browser with an HTTP API, implemented in Python 3 using Twisted 
 * and QT5. The (twisted) QT reactor is used to make the service fully asynchronous allowing to take advantage of webkit concurrency \
 * via QT main loop.
 * 
 * @param splashHost the host name or IP address of the Splash instance. Defaults to "`localhost`" 
 * @param splashPort the port number the Splash instance is running on. Defaults to `8050`
 * @param splashUser the username use if authentication is enabled in the Splash instance. Keep `null` for no authentication
 * @param splashPass the password use if authentication is enabled in the Splash instance. Keep `null` for no authentication
 * @param useSSL if `true` the connection to the Splash intance will be made over `HTTPS`
 * @example
 * import splish.Splash
 
 * Splash().renderHTML("https://https://www.scala-lang.org/")
 * 
 * @see [[https://splash.readthedocs.io/en/stable/api.html The Splash Official API Documentation]]
 */
case class Splash(private val splashHost: String = "localhost", 
                  private val splashPort: Integer = 8050,
                  private val splashUser: String = null,
                  private val splashPassword: String = null,
                  private val useSSL:Boolean  = false) {
  
  private var scheme : String = if (useSSL) "https" else "http"
  private var splashHostUrlPrefix:String = s"$scheme://$splashHost:$splashPort"
  private var auth : RequestAuth = (if (splashUser == null) RequestAuth.Empty else new RequestAuth.Basic(splashUser, splashPassword))

  private def splashLogical(x : Boolean = true) = { if (x) "1" else "0" }

  private def processParams(urlToRender: String, 
                 baseURL: String = null,
                 timeout: Double = 30, 
                 resourceTimeout : Double = 0,
                 wait: Double = 0,
                 proxy: String = null,
                 viewport: String = null,
                 js: String = null,
                 jsSource: String = null,
                 filters: String = null,
                 allowedDomains: String = null,
                 allowedContentTypes: String = null,
                 forbiddenContentTypes: String = null,
                 images: Boolean = true) : Map[String,String] = {

    var params : Map[String, String] = Map(
        "url" -> urlToRender,
        "timeout" -> timeout.toString, 
        "resource_timeout" -> resourceTimeout.toString, 
        "wait" -> wait.toString,
        "images" -> splashLogical(images)
      )

    if (proxy != null) params = params + ("proxy" -> proxy)
    if (viewport != null) params = params + ("viewport" -> viewport)
    if (js != null) params = params + ("js" -> js)
    if (jsSource != null) params = params + ("js_source" -> jsSource)
    if (filters != null) params = params + ("filters" -> filters)
    if (allowedDomains != null) params = params + ("allowed_domains" -> allowedDomains)
    if (allowedContentTypes != null) params = params + ("allowed_content_types" -> allowedContentTypes)
    if (forbiddenContentTypes != null) params = params + ("forbidden_content_types" -> forbiddenContentTypes)

    params

  }

/**
  * Return the HTML of the javascript-rendered page as a String
  *
  * @param urlToRender The url to render (required)
  * @param baseURL The base url to render the page with. Base HTML content will be fetched from 
  *                the URL given in the url argument, while relative referenced resources in the
  *                HTML-text used to render the page are fetched using the URL given in the baseURL
  *                argument as base.
  * @param timeout A timeout (in seconds) for the render (defaults to 30). By default, maximum 
  *                allowed value for the timeout is 90 seconds.
  * @param resourceTimeout A timeout (in seconds) for individual network requests.
  * @param wait Time (in seconds) to wait for updates after page is loaded (defaults to 0). 
  *             Increase this value if you expect pages to contain setInterval/setTimeout javascript calls
  *             because with wait=0 callbacks of setInterval/setTimeout won’t be executed. Non-zero wait 
  *             is also required for PNG and JPEG rendering when doing full-page rendering 
  * @param proxy Proxy profile name or proxy URL. A proxy URL should have the following format: 
  *             "`[protocol://][user:password@]proxyhost[:port]`"
  * @param viewport (String) View width and height (in pixels) of the browser viewport to render the web page.
  *                 Format is “`<width>x<height>`”, e.g. "`800x600`". Default value is "`1024x768`".
  * @param js Javascript profile name. 
  * @param jsSource JavaScript code to be executed in page context. 
  * @param filters (String) Comma-separated list of request filter names.
  * @param allowedDomains (String) Comma-separated list of allowed domain names. If present, 
  *                       Splash won’t load anything neither from domains not in this list nor 
  *                       from subdomains of domains not in this list.
  * @param allowedContentTypes (String) Comma-separated list of allowed content types. If present, 
  *                            Splash will abort any request if the response’s content type doesn’t 
  *                            match any of the content types in this list. Wildcards are supported 
  *                            using Python's fnmatch syntax.
  * @param forbiddenContentTypes (String) Comma-separated list of forbidden content types. If present, 
  *                              Splash will abort any request if the response’s content type doesn’t 
  *                              match any of the content types in this list. Wildcards are supported 
  *                               using Python's fnmatch syntax.
  * @param images Whether to download images. 
  * @see [[https://splash.readthedocs.io/en/stable/api.html#render-html The Official Splash API documentation for render.html endpoint]]
  * @return String containing HTML content
  */
  def renderHTML(urlToRender: String, 
                 baseURL: String = null,
                 timeout: Double = 30, 
                 resourceTimeout : Double = 0,
                 wait: Double = 0,
                 proxy: String = null,
                 viewport: String = null,
                 js: String = null,
                 jsSource: String = null,
                 filters: String = null,
                 allowedDomains: String = null,
                 allowedContentTypes: String = null,
                 forbiddenContentTypes: String = null,
                 images: Boolean = true): String = {

    val splashURL = s"$splashHostUrlPrefix/render.html"

    val res = requests.get(
      splashURL, 
      auth = auth,
      params = processParams(urlToRender, baseURL, timeout, resourceTimeout, wait, 
                             proxy, viewport, js, jsSource, filters, allowedDomains, 
                             allowedContentTypes, forbiddenContentTypes, images)
    )

    res.text

  } 


/**
  * Return information about Splash interaction with a website in [[http://www.softwareishard.com/blog/har-12-spec/ HAR]]
  * format. It includes information about requests made, responses received, timings, headers, etc.
  * 
  * Currently this endpoint doesn’t expose raw request contents; only meta-information like headers and timings is available.
  * Response contents is included when `responseBody` is `true`.
  *
  * @param urlToRender The url to render (required)
  * @param responseBody If `true` then response content is included in the HAR records; The default if `false`
  * @see `renderHTML()` for documentation on the additional parameters
  * @see [[https://splash.readthedocs.io/en/stable/api.html#render-har The Official Splash API documentation for render.har endpoint]]
  * @return parsed JSON
  */
  def renderHAR(urlToRender: String, 
                responseBody : Boolean = false,
                baseURL: String = null,
                timeout: Double = 30, 
                resourceTimeout : Double = 0,
                wait: Double = 0,
                proxy: String = null,
                viewport: String = null,
                js: String = null,
                jsSource: String = null,
                filters: String = null,
                allowedDomains: String = null,
                allowedContentTypes: String = null,
                forbiddenContentTypes: String = null,
                images: Boolean = true): ujson.Js = {

    val splashURL = s"$splashHostUrlPrefix/render.har"

    var params = processParams(urlToRender, baseURL, timeout, resourceTimeout, wait, 
                             proxy, viewport, js, jsSource, filters, allowedDomains, 
                             allowedContentTypes, forbiddenContentTypes, images)

    params = params + ("response_body" -> splashLogical(responseBody))

    val res = requests.get(
      splashURL, 
      auth = auth,
      params = params
    )

    ujson.Js(res.text)

  } 



/**
  * Return a parsed, JSON-encoded dictionary with information about JavaScript-rendered webpage. 
  * It can include HTML, PNG and other information, based on arguments passed.
  *
  * @param urlToRender The url to render (required)
  * @param responseBody If `true` then response content is included in the HAR records; The default if `false`
  * @param html Whether to include HTML in output
  * @param png Whether to include PNG in output
  * @param jpeg Whether to include JPEG in output
  * @param iframes Whether to include information about child frames in output
  * @param script Whether to include the result of the executed javascript final statement in output
  * @param console Whether to include the executed javascript console messages in output
  * @param history Whether to include the history of requests/responses for webpage main frame
  * @param har Whether to include HAR in output. If this option is `true` the result will contain the same data 
  *            as `renderHAR()` provides under `har` key.
  * @see `renderHTML()` for documentation on the additional parameters
  * @see [[https://splash.readthedocs.io/en/stable/api.html#render-json The Official Splash API documentation for render.json endpoint]]
  */
  def renderJSON(urlToRender: String, 
                responseBody : Boolean = false,
                html : Boolean = false,
                png : Boolean = false,
                jpeg : Boolean = false,
                iframes : Boolean = false,
                script : Boolean = false,
                console : Boolean = false,
                history : Boolean = false,
                har : Boolean = false,
                baseURL: String = null,
                timeout: Double = 30, 
                resourceTimeout : Double = 0,
                wait: Double = 0,
                proxy: String = null,
                viewport: String = null,
                js: String = null,
                jsSource: String = null,
                filters: String = null,
                allowedDomains: String = null,
                allowedContentTypes: String = null,
                forbiddenContentTypes: String = null,
                images: Boolean = true): ujson.Js = {

    val splashURL = s"$splashHostUrlPrefix/render.json"

    var params = processParams(urlToRender, baseURL, timeout, resourceTimeout, wait, 
                             proxy, viewport, js, jsSource, filters, allowedDomains, 
                             allowedContentTypes, forbiddenContentTypes, images)

    params = params + ("response_body" -> splashLogical(responseBody))
    params = params + ("html" -> splashLogical(html))
    params = params + ("png" -> splashLogical(png))
    params = params + ("jpeg" -> splashLogical(jpeg))
    params = params + ("iframes" -> splashLogical(iframes))
    params = params + ("script" -> splashLogical(script))
    params = params + ("console" -> splashLogical(console))
    params = params + ("history" -> splashLogical(history))
    params = params + ("har" -> splashLogical(har))

    val res = requests.get(
      splashURL, 
      auth = auth,
      params = params
    )

    ujson.Js(res.text)

  } 

/**
 * Execute a custom rendering script and return a result.
 *
 * The "render" endpoints cover many common use cases, but are occassionally insufficient for a given task.
 * This API endpoint interface allows the caller to write custom 
 * [[https://splash.readthedocs.io/en/stable/scripting-tutorial.html#scripting-tutorial Splash Scripts]].
 *
 * These are complete Lua scripts that must include a `function main(splash, args) ... end` in the body.
 * See the sibling method `run()` for an equivalent method that provides that boilerplate for you.
 *
 * @param luaSource The browser automation script. See the 
 *                  [[https://splash.readthedocs.io/en/stable/scripting-tutorial.html#scripting-tutorial Splash Scripts Tutorial]] 
 *                  for more information.
 * @param timeout A timeout (in seconds) for the render (defaults to 30). By default, maximum 
 *                allowed value for the timeout is 90 seconds.
 * @param allowedDomains (String) Comma-separated list of allowed domain names. If present, 
 *                       Splash won’t load anything neither from domains not in this list nor 
 *                       from subdomains of domains not in this list.
 * @param proxy Proxy profile name or proxy URL. A proxy URL should have the following format: 
 *             "`[protocol://][user:password@]proxyhost[:port]`"
 * @param filters (String) Comma-separated list of request filter names.
 * @param luaArgs (Map[String,String]) additional arguments to be passed to the lua script These will be available in a
 *                [[https://splash.readthedocs.io/en/stable/scripting-ref.html#splash-args splash.args]] table.
 * @return a `Response` object since there's no way for the function to know what the script will return. You will need
 *         to process the `text` element.
 */
 def execute(luaSource : String,
             timeout: Double = 30, 
             allowedDomains: String = null,
             proxy: String = null,
             filters: String = null,
             luaArgs: Map[String,String] = null): Response = {

    val splashURL = s"$splashHostUrlPrefix/execute"

    var params = Map(
      "lua_source" -> luaSource,
      "timeout" -> timeout.toString
    )

    if (allowedDomains != null) params = params + ("allowed_domains" -> allowedDomains)
    if (proxy != null) params = params + ("proxy" -> proxy)
    if (filters != null) params = params + ("filters" -> filters)
    if (luaArgs != null) params = params ++ (for((k,v) <- luaArgs) yield (k -> (v + params.getOrElse(k,0))))

    val res = requests.get(
      splashURL, 
      auth = auth,
      params = params
    )

    res

 }

/**
 * Execute a custom rendering script and return a result.
 *
 * This is nearly identical to `execute()` but it provided the boilerplate `function main(splash, args) ... end`
 *
 * @param luaSource The browser automation script. See the 
 *                  [[https://splash.readthedocs.io/en/stable/scripting-tutorial.html#scripting-tutorial Splash Scripts Tutorial]] 
 *                  for more information.
 * @param timeout A timeout (in seconds) for the render (defaults to 30). By default, maximum 
 *                allowed value for the timeout is 90 seconds.
 * @param allowedDomains (String) Comma-separated list of allowed domain names. If present, 
 *                       Splash won’t load anything neither from domains not in this list nor 
 *                       from subdomains of domains not in this list.
 * @param proxy Proxy profile name or proxy URL. A proxy URL should have the following format: 
 *             "`[protocol://][user:password@]proxyhost[:port]`"
 * @param filters (String) Comma-separated list of request filter names.
 * @param luaArgs (Map[String,String]) additional arguments to be passed to the lua script These will be available in a
 *                [[https://splash.readthedocs.io/en/stable/scripting-ref.html#splash-args splash.args]] table.
 * @return a `Response` object since there's no way for the function to know what the script will return. You will need to process
 *         the `text` element.
 */
 def run(luaSource : String,
         timeout: Double = 30, 
         allowedDomains: String = null,
         proxy: String = null,
         filters: String = null,
         luaArgs: Map[String,String] = null): Response = {

    val splashURL = s"$splashHostUrlPrefix/run"

    var params = Map(
      "lua_source" -> luaSource,
      "timeout" -> timeout.toString
    )

    if (allowedDomains != null) params = params + ("allowed_domains" -> allowedDomains)
    if (proxy != null) params = params + ("proxy" -> proxy)
    if (filters != null) params = params + ("filters" -> filters)
    if (luaArgs != null) params = params ++ (for((k,v) <- luaArgs) yield (k -> (v + params.getOrElse(k,0))))

    val res = requests.get(
      splashURL, 
      auth = auth,
      params = params
    )

    res

 }


/**
  * Test whether the Splash instance is responding
  *
  * @return `true` if the Splash server could be reached and responded affirmatively.
  */
  def isActive() : Boolean = {

    val splashURL = s"$splashHostUrlPrefix/_ping"

    val res = requests.get(
      splashURL, 
      auth = auth
    )

    res.statusCode == 200
  
  }

/**
  * Run Python garbage collector in the Splash instance and clear internal WebKit caches.
  *
  * @return information about the number of objects freed and the status of the Splash instance
  */
  def reset() : ujson.Js = {

    val splashURL = s"$splashHostUrlPrefix/_gc"

    val res = requests.post(
      splashURL, 
      auth = auth
    )
  
    ujson.Js(res.text)

  }

/**
  * Retrieve debug-level information for the Splash instance
  *
  * @return the Splash debug-level information (a `ujson.Js` parsed object)
  */
  def debugInfo() : ujson.Js = {

    val splashURL = s"$splashHostUrlPrefix/_debug"

    val res = requests.get(
      splashURL, 
      auth = auth
    )

    ujson.Js(res.text)
  
  }

/**
  * Retrieve the version information from a running Splash instance
  *
  * @return the Splash version information (a `ujson.Js` parsed object)
  */
  def version() : ujson.Js = {

    var res = run("return splash:get_version()")

    ujson.Js(res.text)
  
  }

/**
  * Retrieve peformance-related statistics for the running Splash instance
  *
  * @return the Splash performance statistics (a `ujson.Js` parsed object)
  */
  def performanceStatistics() : ujson.Js = {

    var res = run("return splash:get_perf_stats()")

    ujson.Js(res.text)
  
  }

/**
  * Retrieve information about requests/responses for the pages loaded by the Splash instance
  *
  * @return the Splash history information(a `ujson.Js` parsed object)
  */
  def history() : ujson.Js = {

    var res = run("return splash:history()")

    ujson.Js(res.text)
  
  }


}


object SplashMain {

  case class Config(
    host: String = "localhost",
    port: Integer = 8050,
    user: String = null,
    pass: String = null,
    ssl: Boolean = false,
    delay: Double = 2.0,
    timeout: Double = 30.0,
    render: String = "html",
    url: String = null
  )

  def main(args: Array[String]) {

    val parser = new scopt.OptionParser[Config]("splash") {

      head("splash", "1.0")

      arg[String]("url").action((x, c) => c.copy(url = x)).
      text("the URL to scrape")
      
      opt[String]('r', "render").optional().valueName("html").
        action((x, c) => c.copy(render = x)).
        validate( x =>
          x match {
            case "html" => success
            case "json" => success
            case "har" => success
            case _ => failure("Option --render must be one of [html|json|har] and defaults to 'html' if not specified")
          }).
        text("request action; one of 'html', 'json' or 'har'")

      help("help").text("prints this usage text")

      opt[Double]('w', "wait").action((x, c) => c.copy(delay = x)).
      text("How long to wait (in seconds) after loading the page (to allow js onX scripts to run). Default is 2 seconds")

      opt[Double]('t', "timeout").action((x, c) => c.copy(timeout = x)).
      text("Overall page/connection timeout. Defaults to 30 seconds")

      opt[String]('h', "host").optional().action((x, c) => c.copy(host = x)).
        text("Splash instance host name or IP address (defaults to localhost)")

      opt[Int]('p', "port").optional().action((x, c) => c.copy(port = x)).
        text("Splash instance port if not the default (8050)")
      
      opt[String]('u', "user").optional().action((x, c) => c.copy(user = x)).
        text("Splash username (if authentication is required). Default is no authentcation.")
      
      opt[String]('p', "pass").optional().action((x, c) => c.copy(pass = x)).
        text("Splash password (if authentication is required). Default is no authentication.")
      
      opt[Unit]('s', "ssl").optional().action((x, c) => c.copy(ssl = true)).
        text("Use an SSL connection to the Splash instance? (defaults to false)")

    }

    parser.parse(args, Config()) match {

      case Some(config) => {

        val s = Splash(config.host, config.port, config.user, config.pass, config.ssl)

        if (!s.isActive()) {
          System.err.println("Splash instance is not active")
          System.exit(-1)
        }

        s.reset()

        config.render match {
          case "html" => println(s.renderHTML(
            config.url, wait = config.delay, timeout = config.timeout)
          )
          case "har" => println(s.renderHAR(
            config.url, responseBody = true, wait = config.delay, timeout = config.timeout)
          )
          case "json" => println(s.renderJSON(
            config.url, responseBody = true, html = true, png = true, jpeg = true,
            iframes = true, wait = config.delay, timeout = config.timeout)
          )
        }

      }

      case None => {
        System.err.println("An unknown error occurred")
        System.exit(-1)
      }

    }

  }

}