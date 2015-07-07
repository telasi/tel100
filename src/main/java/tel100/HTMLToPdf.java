package tel100;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import org.apache.commons.io.IOUtils;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BarcodeQRCode;
import com.itextpdf.text.pdf.BaseFont;
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
    if (args.length != 7 && args.length != 9)
      throw new IllegalArgumentException("usage: java tel100.HtmlToPdf <locale> <src> <dest> <marginTop> <marginRight> <marginBottom> <marginLeft> {<number> <date>}");
    String locale = args[0];
    String src = args[1];
    String dest = args[2];
    float[] margins = { Float.parseFloat(args[3]), Float.parseFloat(args[4]), Float.parseFloat(args[5]), Float.parseFloat(args[6]) };
    String number = null;
    String date = null;
    if (args.length == 9) {
      number = args[7];
      date = args[8];
    }
    convert(locale, src, dest, margins, number, date);
  }

  public static void convert(String locale, String src, String dest, float[] margins, String number, String date) throws Exception {
    Document document = new Document(PageSize.A4);

    float marginTop = margins[0] * INCHES_TO_POINTS;
    float marginRight = margins[1] * INCHES_TO_POINTS;
    float marginBottom = margins[2] * INCHES_TO_POINTS;
    float marginLeft = margins[3] * INCHES_TO_POINTS;
    document.setMargins(marginLeft, marginRight, marginTop, marginBottom);
    FontFactory.register("fonts/DejaVuSerif.ttf", "dejavu");
    FontFactory.register("fonts/DejaVuSerif-Bold.ttf", "dejavu-bold");

    PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(dest));
    document.open();

    // header part

    if (date != null && number != null) {
      // bar code
      Rectangle rect = document.getPageSize();
      BarcodeQRCode barcode = new BarcodeQRCode("tel100-" + number + "-" + date, 1, 1, null);
      Image barcodeImage = barcode.getImage();
      barcodeImage.scalePercent(300);
      barcodeImage.setAbsolutePosition(marginLeft, rect.getTop() - marginTop - barcodeImage.getScaledHeight());
      document.add(barcodeImage);

      Font font = FontFactory.getFont("dejavu", BaseFont.IDENTITY_H, true);
      Font fontBold = FontFactory.getFont("dejavu-bold", BaseFont.IDENTITY_H, true);
      font.setSize(10);
      fontBold.setSize(10);

      boolean isKa = locale == "ka";

      Paragraph p1 = new Paragraph();
      p1.add(new Chunk(isKa ? "ნომერი: " : "Номер: ", fontBold));
      p1.add(new Chunk(number, font));
      p1.setAlignment(Paragraph.ALIGN_RIGHT);

      Paragraph p2 = new Paragraph();
      p2.add(new Chunk(isKa ? "თარიღი: " : "Дата: ", fontBold));
      p2.add(new Chunk(date, font));
      p2.setAlignment(Paragraph.ALIGN_RIGHT);

      document.add(p1);
      document.add(p2);

      Paragraph p3 = new Paragraph(" ");
      p3.setSpacingAfter(barcodeImage.getScaledHeight() - p1.getLeading() - p2.getLeading());
      document.add(p3);
    }

    // main body

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
    // removing font-family tags
    html = html.replaceAll("font-family: [^;]+;", "");
    return new BufferedReader(new StringReader(html));
  }
}
