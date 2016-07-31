package com.io7m.kstructural.tests.plain;

import com.io7m.jorchard.core.JOTreeNodeType;
import com.io7m.jptbox.core.JPTextImageType;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument;
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument.KSBlockDocumentWithParts;
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument.KSBlockDocumentWithSections;
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockParagraph;
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSection;
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSection.KSBlockSectionWithContent;
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSection.KSBlockSectionWithSubsections;
import com.io7m.kstructural.core.KSSubsectionContent;
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionFootnote;
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionFormalItem;
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionParagraph;
import com.io7m.kstructural.core.evaluator.KSEvaluation;
import com.io7m.kstructural.frontend.KSOpFailed;
import com.io7m.kstructural.frontend.KSParseAndEvaluate;
import com.io7m.kstructural.plain.KSPlainLayout;
import com.io7m.kstructural.plain.KSPlainLayoutBox;
import com.io7m.kstructural.plain.KSPlainRasterizer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

public final class KSPlainDemo
{
  private static final Pattern TRAILING_SPACE = Pattern.compile("\\s+$");

  private KSPlainDemo()
  {
    throw new UnreachableCodeException();
  }

  public static void main(final String[] args)
    throws IOException, KSOpFailed
  {
    final Path file = Paths.get(args[0]);
    final Path path = file.getParent();
    final KSBlockDocument<KSEvaluation> document =
      KSParseAndEvaluate.parseAndEvaluate(path, file);

    if (document instanceof KSBlockDocumentWithSections) {
      KSPlainDemo.mainSections(
        (KSBlockDocumentWithSections<KSEvaluation>) document);
    } else if (document instanceof KSBlockDocumentWithParts) {
      KSPlainDemo.mainParts(
        (KSBlockDocumentWithParts<KSEvaluation>) document);
    } else {
      throw new UnreachableCodeException();
    }
  }

  private static void mainParts(
    final KSBlockDocumentWithParts<KSEvaluation> document)
  {

  }

  private static void mainSections(
    final KSBlockDocumentWithSections<KSEvaluation> document)
  {
    final List<KSBlockSection<KSEvaluation>> sections = document.getContent();
    for (int index = 0; index < sections.size(); ++index) {
      final KSBlockSection<KSEvaluation> section = sections.get(index);
      if (section instanceof KSBlockSectionWithContent) {
        KSPlainDemo.mainSectionWithContent(
          (KSBlockSectionWithContent<KSEvaluation>) section);
      } else if (section instanceof KSBlockSectionWithSubsections) {
        KSPlainDemo.mainSectionWithSubsections(
          (KSBlockSectionWithSubsections<KSEvaluation>) section);
      }
    }
  }

  private static void mainSectionWithSubsections(
    final KSBlockSectionWithSubsections<KSEvaluation> section)
  {

  }

  private static void mainSectionWithContent(
    final KSBlockSectionWithContent<KSEvaluation> section)
  {
    final List<KSSubsectionContent<KSEvaluation>> content = section.getContent();
    for (final KSSubsectionContent<KSEvaluation> c : content) {
      if (c instanceof KSSubsectionParagraph) {
        final KSSubsectionParagraph<KSEvaluation> cc =
          (KSSubsectionParagraph<KSEvaluation>) c;
        KSPlainDemo.mainParagraph(cc.getParagraph());
      } else if (c instanceof KSSubsectionFormalItem) {
        final KSSubsectionFormalItem<KSEvaluation> cc =
          (KSSubsectionFormalItem<KSEvaluation>) c;
      } else if (c instanceof KSSubsectionFootnote) {
        final KSSubsectionFootnote<KSEvaluation> cc =
          (KSSubsectionFootnote<KSEvaluation>) c;
      }
    }
  }

  private static void mainParagraph(
    final KSBlockParagraph<KSEvaluation> paragraph)
  {
    final JOTreeNodeType<KSPlainLayoutBox> layout =
      KSPlainLayout.INSTANCE.layoutParagraph(80, paragraph);
    final JPTextImageType image =
      KSPlainRasterizer.INSTANCE.rasterize(layout);

    final StringBuilder sb = new StringBuilder(image.width());
    for (int y = 0; y < image.height(); ++y) {
      sb.setLength(0);
      for (int x = 0; x < image.width(); ++x) {
        sb.appendCodePoint(image.get(x, y));
      }
      System.out.println(
        KSPlainDemo.TRAILING_SPACE.matcher(sb.toString()).replaceAll(""));
    }

    System.out.println();
  }
}
