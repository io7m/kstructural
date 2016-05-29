/*
 * Copyright © 2016 <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.kstructural.parser.imperative

import com.io7m.jlexing.core.LexicalPositionType
import com.io7m.junreachable.UnreachableCodeException
import com.io7m.kstructural.core.KSElement.KSBlock
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument.KSBlockDocumentWithParts
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockDocument.KSBlockDocumentWithSections
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockFootnote
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockFormalItem
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockImport
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockParagraph
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockPart
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSection
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSection.KSBlockSectionWithContent
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSection.KSBlockSectionWithSubsections
import com.io7m.kstructural.core.KSElement.KSBlock.KSBlockSubsection
import com.io7m.kstructural.core.KSElement.KSInline
import com.io7m.kstructural.core.KSParse
import com.io7m.kstructural.core.KSParseContextType
import com.io7m.kstructural.core.KSParseError
import com.io7m.kstructural.core.KSResult
import com.io7m.kstructural.core.KSSubsectionContent
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionFootnote
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionFormalItem
import com.io7m.kstructural.core.KSSubsectionContent.KSSubsectionParagraph
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeCommand
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeCommand.KSImperativeDocument
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeCommand.KSImperativeFootnote
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeCommand.KSImperativeFormalItem
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeCommand.KSImperativeImport
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeCommand.KSImperativeParagraph
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeCommand.KSImperativePart
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeCommand.KSImperativeSection
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeCommand.KSImperativeSubsection
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeEOF
import com.io7m.kstructural.parser.imperative.KSImperative.KSImperativeInline
import org.slf4j.LoggerFactory
import org.valid4j.Assertive
import java.nio.file.Path
import java.util.Optional

class KSImperativeBuilder private constructor()
: KSImperativeBuilderType {

  companion object {

    private val LOG = LoggerFactory.getLogger(KSImperativeBuilder::class.java)

    private fun unexpectedInline(
      command : KSImperativeInline)
      : KSResult<Optional<KSBlock<KSParse>>, KSParseError> =
      unexpectedElement(
        message = "Unexpected inline content.",
        expected = "A block command",
        received = command,
        position = command.position)

    private fun ellipsize(
      x : Any,
      n : Int) : String {
      val s = x.toString()
      return if (s.length > n) {
        s.substring(0, n) + "..."
      } else {
        s
      }
    }

    private fun unexpectedElement(
      message : String,
      expected : String,
      received : Any,
      position : Optional<LexicalPositionType<Path>>)
      : KSResult<Optional<KSBlock<KSParse>>, KSParseError> {
      val sb = StringBuilder()
      sb.append(message)
      sb.append(System.lineSeparator())
      sb.append("Expected: ")
      sb.append(expected)
      sb.append(System.lineSeparator())
      sb.append("Received: ")
      sb.append(ellipsize(received, 50))
      sb.append(System.lineSeparator())
      return KSResult.fail<Optional<KSBlock<KSParse>>, KSParseError>(
        KSParseError(position, sb.toString()))
    }

    private fun expectedSubsectionContent(
      command : KSImperativeCommand)
      : KSResult<Optional<KSBlock<KSParse>>, KSParseError> =
      unexpectedElement(
        message = "Unexpected block command.",
        expected = "Subsection content",
        received = command,
        position = command.position)

    private fun <T : KSBlock<KSParse>> succeedSomeBlock(
      x : T) : KSResult<Optional<KSBlock<KSParse>>, KSParseError> {
      return KSResult.succeed(Optional.of(x as KSBlock<KSParse>))
    }

    private fun succeedNothing() =
      KSResult.succeed<Optional<KSBlock<KSParse>>, KSParseError>(
        Optional.empty<KSBlock<KSParse>>())

    fun create() : KSImperativeBuilderType =
      KSImperativeBuilder()

  }

  private var builder : KSImperativeBuilderType? = null

  override fun add(
    context : KSParseContextType,
    command : KSImperative) : KSResult<Optional<KSBlock<KSParse>>, KSParseError> =
    if (builder != null) {
      builder!!.add(context, command)
    } else {
      start(context, command)
    }

  private inner class ParagraphBuilder(
    private val command_initial : KSImperativeParagraph)
  : KSImperativeBuilderType {

    init {
      LOG.trace("start paragraph")
    }

    private val content : MutableList<KSInline<KSParse>> = mutableListOf()

    override fun add(
      context : KSParseContextType,
      command : KSImperative)
      : KSResult<Optional<KSBlock<KSParse>>, KSParseError> {
      return when (command) {
        is KSImperativeCommand,
        is KSImperativeEOF    ->
          succeedSomeBlock(finish(context))
        is KSImperativeInline -> {
          content.add(command.value)
          succeedNothing()
        }
      }
    }

    fun finish(
      context : KSParseContextType) : KSBlockParagraph<KSParse> {

      LOG.trace("finish paragraph")
      return KSBlockParagraph(
        command_initial.position,
        command_initial.square,
        KSParse(context),
        command_initial.type,
        command_initial.id,
        content)
    }
  }

  private inner class FormalItemBuilder(
    private val command_initial : KSImperativeFormalItem)
  : KSImperativeBuilderType {

    init {
      LOG.trace("start formal item")
    }

    private val content : MutableList<KSInline<KSParse>> = mutableListOf()

    override fun add(
      context : KSParseContextType,
      command : KSImperative)
      : KSResult<Optional<KSBlock<KSParse>>, KSParseError> {
      return when (command) {
        is KSImperativeCommand,
        is KSImperativeEOF    ->
          succeedSomeBlock(finish(context))
        is KSImperativeInline -> {
          content.add(command.value)
          succeedNothing()
        }
      }
    }

    fun finish(
      context : KSParseContextType) : KSBlockFormalItem<KSParse> {

      LOG.trace("finish formal item")
      return KSBlockFormalItem(
        command_initial.position,
        command_initial.square,
        KSParse(context),
        command_initial.type,
        command_initial.id,
        command_initial.title,
        content)
    }
  }

  private inner class FootnoteBuilder(
    private val command_initial : KSImperativeFootnote)
  : KSImperativeBuilderType {

    init {
      LOG.trace("start footnote")
    }

    private val content : MutableList<KSInline<KSParse>> = mutableListOf()

    override fun add(
      context : KSParseContextType,
      command : KSImperative)
      : KSResult<Optional<KSBlock<KSParse>>, KSParseError> {
      return when (command) {
        is KSImperativeCommand,
        is KSImperativeEOF    ->
          succeedSomeBlock(finish(context))
        is KSImperativeInline -> {
          content.add(command.value)
          succeedNothing()
        }
      }
    }

    fun finish(
      context : KSParseContextType) : KSBlockFootnote<KSParse> {

      LOG.trace("finish footnote")

      return KSBlockFootnote(
        position = command_initial.position,
        square = command_initial.square,
        id_real = command_initial.id_real,
        data = KSParse(context),
        type = command_initial.type,
        content = content)
    }
  }

  private inner class SubsectionBuilder(
    private val command_initial : KSImperativeSubsection)
  : KSImperativeBuilderType {

    init {
      LOG.trace("start subsection")
    }

    private val content : MutableList<KSSubsectionContent<KSParse>> = mutableListOf()
    private var paragraph_builder : ParagraphBuilder? = null
    private var formal_builder : FormalItemBuilder? = null
    private var footnote_builder : FootnoteBuilder? = null

    override fun add(
      context : KSParseContextType,
      command : KSImperative)
      : KSResult<Optional<KSBlock<KSParse>>, KSParseError> {
      return when (command) {
        is KSImperative.KSImperativeCommand -> {
          val cc = command as KSImperativeCommand
          when (cc) {
            is KSImperativeDocument,
            is KSImperativePart,
            is KSImperativeSection,
            is KSImperativeSubsection ->
              expectedSubsectionContent(cc)

            is KSImperativeParagraph  -> {
              finishContentIfNecessary(context)
              this.paragraph_builder = ParagraphBuilder(cc)
              succeedNothing()
            }
            is KSImperativeFootnote   -> {
              finishContentIfNecessary(context)
              this.footnote_builder = FootnoteBuilder(cc)
              succeedNothing()
            }
            is KSImperativeFormalItem -> {
              finishContentIfNecessary(context)
              this.formal_builder = FormalItemBuilder(cc)
              succeedNothing()
            }
            is KSImperativeImport     -> {
              when (cc.content) {
                is KSBlockDocument,
                is KSBlockSection,
                is KSBlockSubsection,
                is KSBlockPart,
                is KSBlockImport     -> {
                  unexpectedElement(
                    message = "Unexpected imported block.",
                    expected = "Subsection content",
                    received = "(Imported) " + cc.content,
                    position = cc.position)
                }
                is KSBlockParagraph  -> {
                  finishContentIfNecessary(context)
                  this.content.add(KSSubsectionParagraph(cc.content))
                  succeedNothing()
                }
                is KSBlockFormalItem -> {
                  finishContentIfNecessary(context)
                  this.content.add(KSSubsectionFormalItem(cc.content))
                  succeedNothing()
                }
                is KSBlockFootnote   -> {
                  finishContentIfNecessary(context)
                  this.content.add(KSSubsectionFootnote(cc.content))
                  succeedNothing()
                }
              }
            }
          }
        }

        is KSImperative.KSImperativeEOF     ->
          succeedSomeBlock(finish(context))

        is KSImperative.KSImperativeInline  -> {
          if (this.paragraph_builder != null) {
            Assertive.require(formal_builder == null)
            Assertive.require(footnote_builder == null)
            this.paragraph_builder!!.add(context, command)
          } else if (this.formal_builder != null) {
            Assertive.require(paragraph_builder == null)
            Assertive.require(footnote_builder == null)
            this.formal_builder!!.add(context, command)
          } else if (this.footnote_builder != null) {
            Assertive.require(paragraph_builder == null)
            Assertive.require(formal_builder == null)
            this.footnote_builder!!.add(context, command)
          } else {
            unexpectedInline(command)
          }
        }
      }
    }

    private fun finishContentIfNecessary(
      context : KSParseContextType) {
      finishParagraphIfNecessary(context)
      finishFormalItemIfNecessary(context)
      finishFootnoteIfNecessary(context)
    }

    private fun finishParagraphIfNecessary(context : KSParseContextType) {
      if (paragraph_builder != null) {
        Assertive.require(formal_builder == null)
        Assertive.require(footnote_builder == null)
        val b = paragraph_builder!!
        content.add(KSSubsectionParagraph(b.finish(context)))
        this.paragraph_builder = null
      }
    }

    private fun finishFootnoteIfNecessary(context : KSParseContextType) {
      if (footnote_builder != null) {
        Assertive.require(formal_builder == null)
        Assertive.require(paragraph_builder == null)
        val b = footnote_builder!!
        content.add(KSSubsectionFootnote(b.finish(context)))
        this.footnote_builder = null
      }
    }

    private fun finishFormalItemIfNecessary(context : KSParseContextType) {
      if (formal_builder != null) {
        Assertive.require(paragraph_builder == null)
        Assertive.require(paragraph_builder == null)
        val b = formal_builder!!
        content.add(KSSubsectionFormalItem(b.finish(context)))
        this.formal_builder = null
      }
    }

    fun finish(
      context : KSParseContextType) : KSBlockSubsection<KSParse> {

      LOG.trace("finish subsection")
      finishContentIfNecessary(context)

      return KSBlockSubsection(
        command_initial.position,
        command_initial.square,
        KSParse(context),
        command_initial.type,
        command_initial.id,
        command_initial.title,
        content)
    }
  }

  private inner class SectionBuilder(
    private val command_initial : KSImperativeSection)
  : KSImperativeBuilderType {

    init {
      LOG.trace("start section")
    }

    private val content : MutableList<KSSubsectionContent<KSParse>> = mutableListOf()
    private val subsections : MutableList<KSBlockSubsection<KSParse>> = mutableListOf()
    private var paragraph_builder : ParagraphBuilder? = null
    private var subsection_builder : SubsectionBuilder? = null
    private var formal_builder : FormalItemBuilder? = null
    private var footnote_builder : FootnoteBuilder? = null

    override fun add(
      context : KSParseContextType,
      command : KSImperative)
      : KSResult<Optional<KSBlock<KSParse>>, KSParseError> {

      return when (command) {
        is KSImperativeCommand -> {
          val cc = command as KSImperativeCommand
          when (cc) {
            is KSImperativePart,
            is KSImperativeSection,
            is KSImperativeDocument   ->
              expectedSubsectionContent(cc)

            is KSImperativeFootnote   -> {
              finishContentIfNecessary(context)

              Assertive.require(paragraph_builder == null)
              Assertive.require(formal_builder == null)
              Assertive.require(footnote_builder == null)

              if (this.subsection_builder != null) {
                this.subsection_builder!!.add(context, command)
              } else {
                this.footnote_builder = FootnoteBuilder(cc)
                succeedNothing()
              }
            }

            is KSImperativeFormalItem -> {
              finishContentIfNecessary(context)

              Assertive.require(paragraph_builder == null)
              Assertive.require(formal_builder == null)
              Assertive.require(footnote_builder == null)

              if (this.subsection_builder != null) {
                this.subsection_builder!!.add(context, command)
              } else {
                this.formal_builder = FormalItemBuilder(cc)
                succeedNothing()
              }
            }

            is KSImperativeParagraph  -> {
              finishContentIfNecessary(context)

              Assertive.require(paragraph_builder == null)
              Assertive.require(formal_builder == null)
              Assertive.require(footnote_builder == null)

              if (this.subsection_builder != null) {
                this.subsection_builder!!.add(context, command)
              } else {
                this.paragraph_builder = ParagraphBuilder(cc)
                succeedNothing()
              }
            }

            is KSImperativeSubsection -> {
              finishContentIfNecessary(context)

              if (content.isNotEmpty()) {
                unexpectedElement(
                  message = "Unexpected subsection.",
                  expected = "Subsection content",
                  received = command,
                  position = command.position)
              } else {
                finishSubsectionIfNecessary(context)
                this.subsection_builder = SubsectionBuilder(cc)
                succeedNothing()
              }
            }

            is KSImperativeImport     -> {
              when (cc.content) {
                is KSBlockImport,
                is KSBlockDocument,
                is KSBlockSection,
                is KSBlockPart       -> {
                  unexpectedElement(
                    message = "Unexpected imported block.",
                    expected = "Section content",
                    received = "(Imported) " + cc.content,
                    position = cc.position)
                }

                is KSBlockSubsection -> {
                  finishContentIfNecessary(context)

                  if (content.isNotEmpty()) {
                    unexpectedElement(
                      message = "Unexpected imported subsection.",
                      expected = "Subsection content",
                      received = "(Imported) " + cc.content,
                      position = cc.position)
                  } else {
                    subsections.add(cc.content)
                    succeedNothing()
                  }
                }

                is KSBlockFormalItem,
                is KSBlockFootnote,
                is KSBlockParagraph  -> {
                  if (subsection_builder != null) {
                    this.subsection_builder!!.add(context, command)
                  } else {
                    finishContentIfNecessary(context)

                    if (subsections.isNotEmpty()) {
                      unexpectedElement(
                        message = "Unexpected imported subsection content.",
                        expected = "A subsection",
                        received = "(Imported) " + cc.content,
                        position = cc.position)
                    } else {
                      content.add(when (cc.content) {
                        is KSBlockDocument,
                        is KSBlockSection,
                        is KSBlockSubsection,
                        is KSBlockPart,
                        is KSBlockImport     ->
                          throw UnreachableCodeException()
                        is KSBlockParagraph  -> KSSubsectionParagraph(cc.content)
                        is KSBlockFormalItem -> KSSubsectionFormalItem(cc.content)
                        is KSBlockFootnote   -> KSSubsectionFootnote(cc.content)
                      })
                      succeedNothing()
                    }
                  }
                }
              }
            }
          }
        }

        is KSImperativeEOF     ->
          succeedSomeBlock(finish(context))

        is KSImperativeInline  -> {
          if (subsection_builder != null) {
            Assertive.require(paragraph_builder == null)
            Assertive.require(formal_builder == null)
            Assertive.require(footnote_builder == null)
            this.subsection_builder!!.add(context, command)
          } else if (paragraph_builder != null) {
            Assertive.require(subsection_builder == null)
            Assertive.require(formal_builder == null)
            Assertive.require(footnote_builder == null)
            paragraph_builder!!.add(context, command)
          } else if (formal_builder != null) {
            Assertive.require(subsection_builder == null)
            Assertive.require(paragraph_builder == null)
            Assertive.require(footnote_builder == null)
            formal_builder!!.add(context, command)
          } else if (footnote_builder != null) {
            Assertive.require(subsection_builder == null)
            Assertive.require(paragraph_builder == null)
            Assertive.require(formal_builder == null)
            footnote_builder!!.add(context, command)
          } else {
            unexpectedInline(command)
          }
        }
      }
    }

    fun finish(context : KSParseContextType) : KSBlockSection<KSParse> {
      LOG.trace("finish section")

      finishSubsectionIfNecessary(context)
      finishContentIfNecessary(context)

      return if (content.isNotEmpty()) {
        Assertive.require(subsections.isEmpty())
        KSBlockSectionWithContent(
          command_initial.position,
          command_initial.square,
          KSParse(context),
          command_initial.type,
          command_initial.id,
          command_initial.title,
          content)
      } else {
        Assertive.require(content.isEmpty())
        KSBlockSectionWithSubsections(
          command_initial.position,
          command_initial.square,
          KSParse(context),
          command_initial.type,
          command_initial.id,
          command_initial.title,
          subsections)
      }
    }

    private fun finishSubsectionIfNecessary(context : KSParseContextType) {
      if (subsection_builder != null) {
        val p = subsection_builder!!
        subsections.add(p.finish(context))
        this.subsection_builder = null
      }
    }

    private fun finishContentIfNecessary(
      context : KSParseContextType) {
      finishParagraphIfNecessary(context)
      finishFormalIfNecessary(context)
      finishFootnoteIfNecessary(context)
    }

    private fun finishParagraphIfNecessary(context : KSParseContextType) {
      if (paragraph_builder != null) {
        Assertive.require(formal_builder == null)
        Assertive.require(footnote_builder == null)
        Assertive.require(subsections.isEmpty())
        val p = paragraph_builder!!
        content.add(KSSubsectionParagraph(p.finish(context)))
        this.paragraph_builder = null
      }
    }

    private fun finishFormalIfNecessary(context : KSParseContextType) {
      if (formal_builder != null) {
        Assertive.require(paragraph_builder == null)
        Assertive.require(footnote_builder == null)
        Assertive.require(subsections.isEmpty())
        val p = formal_builder!!
        content.add(KSSubsectionFormalItem(p.finish(context)))
        this.formal_builder = null
      }
    }

    private fun finishFootnoteIfNecessary(context : KSParseContextType) {
      if (footnote_builder != null) {
        Assertive.require(paragraph_builder == null)
        Assertive.require(formal_builder == null)
        Assertive.require(subsections.isEmpty())
        val p = footnote_builder!!
        content.add(KSSubsectionFootnote(p.finish(context)))
        this.footnote_builder = null
      }
    }
  }

  private inner class PartBuilder(
    private val command_initial : KSImperativePart)
  : KSImperativeBuilderType {

    private val sections : MutableList<KSBlockSection<KSParse>> = mutableListOf()
    private var section_builder : SectionBuilder? = null

    override fun add(
      context : KSParseContextType,
      command : KSImperative)
      : KSResult<Optional<KSBlock<KSParse>>, KSParseError> {
      return when (command) {
        is KSImperativeCommand -> {
          val cc = command as KSImperativeCommand
          when (cc) {
            is KSImperativeParagraph,
            is KSImperativeFootnote,
            is KSImperativeDocument,
            is KSImperativeSubsection,
            is KSImperativeFormalItem -> {
              if (this.section_builder != null) {
                this.section_builder!!.add(context, command)
              } else {
                unexpectedElement(
                  message = "Unexpected block command.",
                  expected = "A section",
                  received = command,
                  position = command.position)
              }
            }

            is KSImperativePart       -> {
              unexpectedElement(
                message = "Unexpected block command.",
                expected = "A section or section content",
                received = command,
                position = command.position)
            }

            is KSImperativeSection    -> {
              finishSectionIfNecessary(context)
              this.section_builder = SectionBuilder(cc)
              succeedNothing()
            }

            is KSImperativeImport     -> {
              when (cc.content) {
                is KSBlockSection  -> {
                  finishSectionIfNecessary(context)
                  this.sections.add(cc.content)
                  succeedNothing()
                }
                is KSBlockDocument -> {
                  unexpectedElement(
                    message = "Unexpected imported block.",
                    expected = "A section or section content",
                    received = "(Imported) " + cc.content,
                    position = command.position)
                }
                is KSBlockSubsection,
                is KSBlockParagraph,
                is KSBlockFormalItem,
                is KSBlockFootnote,
                is KSBlockPart,
                is KSBlockImport   -> {
                  if (this.section_builder != null) {
                    this.section_builder!!.add(context, command)
                  } else {
                    unexpectedElement(
                      message = "Unexpected imported block.",
                      expected = "A section",
                      received = "(Imported) " + cc.content,
                      position = command.position)
                  }
                }
              }
            }
          }
        }
        is KSImperativeEOF     ->
          succeedSomeBlock(finish(context))

        is KSImperativeInline  -> {
          if (section_builder != null) {
            this.section_builder!!.add(context, command)
          } else {
            unexpectedInline(command)
          }
        }
      }
    }

    fun finish(context : KSParseContextType) : KSBlockPart<KSParse> {
      LOG.trace("finish part")

      finishSectionIfNecessary(context)

      return KSBlockPart(
        command_initial.position,
        command_initial.square,
        KSParse(context),
        command_initial.type,
        command_initial.id,
        command_initial.title,
        sections)
    }

    private fun finishSectionIfNecessary(context : KSParseContextType) {
      if (section_builder != null) {
        val p = section_builder!!
        sections.add(p.finish(context))
        this.section_builder = null
      }
    }

    init {
      LOG.trace("start part")
    }
  }

  private inner class DocumentBuilder(
    private val command_initial : KSImperativeDocument)
  : KSImperativeBuilderType {

    init {
      LOG.trace("start document")
    }

    private val parts : MutableList<KSBlockPart<KSParse>> = mutableListOf()
    private val sections : MutableList<KSBlockSection<KSParse>> = mutableListOf()
    private var section_builder : SectionBuilder? = null
    private var part_builder : PartBuilder? = null

    override fun add(
      context : KSParseContextType,
      command : KSImperative)
      : KSResult<Optional<KSBlock<KSParse>>, KSParseError> {
      return when (command) {

        is KSImperativeCommand -> {
          val cc = command as KSImperativeCommand
          when (cc) {
            is KSImperativeParagraph,
            is KSImperativeFootnote,
            is KSImperativeDocument,
            is KSImperativeSubsection,
            is KSImperativeFormalItem -> {
              if (this.section_builder != null) {
                this.section_builder!!.add(context, command)
              } else if (this.part_builder != null) {
                this.part_builder!!.add(context, command)
              } else {
                unexpectedElement(
                  message = "Unexpected block command.",
                  expected = "A section or part",
                  received = command,
                  position = command.position)
              }
            }

            is KSImperativePart       -> {
              finishContentIfNecessary(context)

              if (sections.isNotEmpty()) {
                unexpectedElement(
                  message = "Unexpected section.",
                  expected = "A section",
                  received = command,
                  position = command.position)
              } else {
                this.part_builder = PartBuilder(cc)
                succeedNothing()
              }
            }

            is KSImperativeSection    -> {
              if (part_builder != null) {
                this.part_builder!!.add(context, command)
              } else {
                finishContentIfNecessary(context)

                if (parts.isNotEmpty()) {
                  unexpectedElement(
                    message = "Unexpected section.",
                    expected = "A part",
                    received = command,
                    position = command.position)
                } else {
                  this.section_builder = SectionBuilder(cc)
                  succeedNothing()
                }
              }
            }

            is KSImperativeImport     -> {
              when (cc.content) {
                is KSBlockDocument -> {
                  unexpectedElement(
                    message = "Unexpected imported block.",
                    expected = "A part, section, or section content",
                    received = "(Imported) " + command,
                    position = command.position)
                }

                is KSBlockSection  -> {
                  if (part_builder != null) {
                    this.part_builder!!.add(context, command)
                  } else {
                    finishContentIfNecessary(context)

                    if (parts.isNotEmpty()) {
                      unexpectedElement(
                        message = "Unexpected imported block.",
                        expected = "A part",
                        received = "(Imported) " + command,
                        position = command.position)
                    } else {
                      this.sections.add(cc.content)
                      succeedNothing()
                    }
                  }
                }

                is KSBlockPart     -> {
                  finishContentIfNecessary(context)

                  if (sections.isNotEmpty()) {
                    unexpectedElement(
                      message = "Unexpected imported block.",
                      expected = "A section",
                      received = "(Imported) " + command,
                      position = command.position)
                  } else {
                    this.parts.add(cc.content)
                    succeedNothing()
                  }
                }

                is KSBlockSubsection,
                is KSBlockParagraph,
                is KSBlockFormalItem,
                is KSBlockFootnote,
                is KSBlockImport   -> {
                  if (this.section_builder != null) {
                    this.section_builder!!.add(context, command)
                  } else if (this.part_builder != null) {
                    this.part_builder!!.add(context, command)
                  } else {
                    unexpectedElement(
                      message = "Unexpected imported block.",
                      expected = "A section or part",
                      received = "(Imported) " + command,
                      position = command.position)
                  }
                }
              }
            }
          }
        }

        is KSImperativeEOF     ->
          succeedSomeBlock(finish(context))

        is KSImperativeInline  ->
          if (section_builder != null) {
            this.section_builder!!.add(context, command)
          } else if (part_builder != null) {
            this.part_builder!!.add(context, command)
          } else {
            unexpectedInline(command)
          }
      }
    }

    private fun finishContentIfNecessary(context : KSParseContextType) : Unit {
      finishSectionIfNecessary(context)
      finishPartIfNecessary(context)
    }

    private fun finishSectionIfNecessary(context : KSParseContextType) {
      if (section_builder != null) {
        val p = section_builder!!
        sections.add(p.finish(context))
        this.section_builder = null
      }
    }

    private fun finishPartIfNecessary(context : KSParseContextType) {
      if (part_builder != null) {
        val p = part_builder!!
        parts.add(p.finish(context))
        this.part_builder = null
      }
    }

    private fun finish(context : KSParseContextType) : KSBlockDocument<KSParse> {
      LOG.trace("finish document")

      finishContentIfNecessary(context)

      if (parts.isNotEmpty()) {
        Assertive.require(sections.isEmpty())
        return KSBlockDocumentWithParts(
          command_initial.position,
          command_initial.square,
          KSParse(context),
          command_initial.id,
          command_initial.type,
          command_initial.title,
          parts)
      } else {
        Assertive.require(parts.isEmpty())
        return KSBlockDocumentWithSections(
          command_initial.position,
          command_initial.square,
          KSParse(context),
          command_initial.id,
          command_initial.type,
          command_initial.title,
          sections)
      }
    }
  }

  private fun start(
    context : KSParseContextType,
    command : KSImperative)
    : KSResult<Optional<KSBlock<KSParse>>, KSParseError> {
    Assertive.require(this.builder == null)
    return when (command) {
      is KSImperativeCommand -> {
        val ee = command as KSImperativeCommand
        when (ee) {
          is KSImperativeDocument   -> {
            this.builder = DocumentBuilder(ee)
            succeedNothing()
          }
          is KSImperativeParagraph  -> {
            this.builder = ParagraphBuilder(ee)
            succeedNothing()
          }
          is KSImperativeFootnote   -> {
            this.builder = FootnoteBuilder(ee)
            succeedNothing()
          }
          is KSImperativePart       -> {
            this.builder = PartBuilder(ee)
            succeedNothing()
          }
          is KSImperativeSection    -> {
            this.builder = SectionBuilder(ee)
            succeedNothing()
          }
          is KSImperativeSubsection -> {
            this.builder = SubsectionBuilder(ee)
            succeedNothing()
          }
          is KSImperativeFormalItem -> {
            this.builder = FormalItemBuilder(ee)
            succeedNothing()
          }
          is KSImperativeImport     ->
            succeedSomeBlock(ee.content)
        }
      }
      is KSImperativeEOF     -> {
        unexpectedElement(
          message = "Unexpected EOF.",
          expected = "A block command",
          received = "EOF",
          position = command.position)
      }
      is KSImperativeInline  ->
        unexpectedInline(command)
    }
  }
}