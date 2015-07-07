package tel100;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import org.apache.commons.io.IOUtils;

import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorker;
import com.itextpdf.tool.xml.XMLWorkerFontProvider;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import com.itextpdf.tool.xml.css.CssFile;
import com.itextpdf.tool.xml.css.StyleAttrCSSResolver;
import com.itextpdf.tool.xml.html.CssAppliers;
import com.itextpdf.tool.xml.html.CssAppliersImpl;
import com.itextpdf.tool.xml.html.Tags;
import com.itextpdf.tool.xml.parser.XMLParser;
import com.itextpdf.tool.xml.pipeline.css.CSSResolver;
import com.itextpdf.tool.xml.pipeline.css.CssResolverPipeline;
import com.itextpdf.tool.xml.pipeline.end.PdfWriterPipeline;
import com.itextpdf.tool.xml.pipeline.html.HtmlPipeline;
import com.itextpdf.tool.xml.pipeline.html.HtmlPipelineContext;

public class HTMLToPdf {
  static final String CSS = "css/tel100.css";
  static final float INCHES_TO_POINTS = 72;

  public static void main(String[] args) throws Exception {
    if (args.length != 6)
      throw new IllegalArgumentException("usage: java tel100.HtmlToPdf <src> <dest> <marginTop> <marginRight> <marginBottom> <marginLeft>");
    String src = args[0];
    String dest = args[1];
    float[] margins = { Float.parseFloat(args[2]), Float.parseFloat(args[3]), Float.parseFloat(args[4]), Float.parseFloat(args[5]) };
    convert(src, dest, margins);
  }

  public static void convert(String src, String dest, float[] margins) throws Exception {
    Document document = new Document();
    float marginTop = margins[0] * INCHES_TO_POINTS;
    float marginRight = margins[1] * INCHES_TO_POINTS;
    float marginBottom = margins[2] * INCHES_TO_POINTS;
    float marginLeft = margins[3] * INCHES_TO_POINTS;
    document.setMargins(marginLeft, marginRight, marginTop, marginBottom);
    PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(dest));
    document.open();
    // CSS, Fonts & HTML
    CSSResolver cssResolver = new StyleAttrCSSResolver();
    InputStream in = ClassLoader.getSystemResource(CSS).openStream();
    CssFile cssFile = XMLWorkerHelper.getCSS(in);
    cssResolver.addCss(cssFile);
    XMLWorkerFontProvider fontProvider = new XMLWorkerFontProvider(XMLWorkerFontProvider.DONTLOOKFORFONTS);
    fontProvider.register("fonts/DejaVuSerif-Bold.ttf");
    fontProvider.register("fonts/DejaVuSerif-BoldItalic.ttf");
    fontProvider.register("fonts/DejaVuSerif.ttf");
    CssAppliers cssAppliers = new CssAppliersImpl(fontProvider);
    HtmlPipelineContext htmlContext = new HtmlPipelineContext(cssAppliers);
    htmlContext.setTagFactory(Tags.getHtmlTagProcessorFactory());
    // Pipelines
    PdfWriterPipeline pdf = new PdfWriterPipeline(document, writer);
    HtmlPipeline html = new HtmlPipeline(htmlContext, pdf);
    CssResolverPipeline css = new CssResolverPipeline(cssResolver, html);
    // XML Worker
    XMLWorker worker = new XMLWorker(css, true);
    XMLParser p = new XMLParser(worker);
    p.parse(readHtml(src));
    document.close();
  }

  /**
   * Read HTML and transform it to be suitable for XMLWorker.
   */
  private static Reader readHtml(String src) throws IOException {
    InputStream in = new FileInputStream(src);
    String html = IOUtils.toString(in);
    // fixing <br> tags
    html = html.replaceAll("<br>", "<br/>");
    // removing <font> tags
    html = html.replaceAll("<font [^>]*>", "");
    html = html.replaceAll("</font>", "");
    return new BufferedReader(new StringReader(html));
  }
}
