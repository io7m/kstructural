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

package com.io7m.kstructural.core.evaluator

sealed class KSNumber {

  interface HasSectionType {
    val section : Long
  }

  interface HasSubsectionType {
    val subsection : Long
  }

  interface HasPartType {
    val part : Long
  }

  interface HasContentType {
    val content : Long
  }

  abstract fun toAnchor() : String

  abstract val least : Long

  class KSNumberPart(
    override val part : Long) : KSNumber(), HasPartType {

    override val least : Long
      get() = part

    override fun toAnchor() : String {
      return "p${part}"
    }

    override fun toString() : String {
      return part.toString()
    }

    override fun equals(other : Any?) : Boolean {
      if (this === other) return true
      if (other?.javaClass != javaClass) return false
      other as KSNumberPart
      if (part != other.part) return false
      return true
    }

    override fun hashCode() : Int {
      return part.hashCode()
    }
  }

  class KSNumberPartSection(
    override val part : Long,
    override val section : Long) : KSNumber(), HasPartType, HasSectionType {

    override val least : Long
      get() = section

    override fun toAnchor() : String {
      return "p${part}s${section}"
    }

    override fun toString() : String {
      val sb = StringBuilder()
      sb.append(part)
      sb.append(".")
      sb.append(section)
      return sb.toString()
    }

    override fun equals(other : Any?) : Boolean {
      if (this === other) return true
      if (other?.javaClass != javaClass) return false
      other as KSNumberPartSection
      if (part != other.part) return false
      if (section != other.section) return false
      return true
    }

    override fun hashCode() : Int {
      var result = part.hashCode()
      result += 31 * result + section.hashCode()
      return result
    }
  }

  class KSNumberPartSectionContent(
    override val part : Long,
    override val section : Long,
    override val content : Long)
  : KSNumber(), HasPartType, HasSectionType, HasContentType {

    override val least : Long
      get() = content

    override fun toAnchor() : String {
      return "p${part}s${section}c${content}"
    }

    override fun toString() : String {
      val sb = StringBuilder()
      sb.append(part)
      sb.append(".")
      sb.append(section)
      sb.append(".")
      sb.append(content)
      return sb.toString()
    }

    override fun equals(other : Any?) : Boolean {
      if (this === other) return true
      if (other?.javaClass != javaClass) return false
      other as KSNumberPartSectionContent
      if (part != other.part) return false
      if (section != other.section) return false
      if (content != other.content) return false
      return true
    }

    override fun hashCode() : Int {
      var result = part.hashCode()
      result += 31 * result + section.hashCode()
      result += 31 * result + content.hashCode()
      return result
    }
  }

  class KSNumberPartSectionSubsection(
    override val part : Long,
    override val section : Long,
    override val subsection : Long)
  : KSNumber(), HasPartType, HasSectionType, HasSubsectionType {

    override val least : Long
      get() = subsection

    override fun toAnchor() : String {
      return "p${part}s${section}ss${subsection}"
    }

    override fun toString() : String {
      val sb = StringBuilder()
      sb.append(part)
      sb.append(".")
      sb.append(section)
      sb.append(".")
      sb.append(subsection)
      return sb.toString()
    }

    override fun equals(other : Any?) : Boolean {
      if (this === other) return true
      if (other?.javaClass != javaClass) return false
      other as KSNumberPartSectionSubsection
      if (part != other.part) return false
      if (section != other.section) return false
      if (subsection != other.subsection) return false
      return true
    }

    override fun hashCode() : Int {
      var result = part.hashCode()
      result += 31 * result + section.hashCode()
      result += 31 * result + subsection.hashCode()
      return result
    }
  }

  class KSNumberPartSectionSubsectionContent(
    override val part : Long,
    override val section : Long,
    override val subsection : Long,
    override val content : Long)
  : KSNumber(), HasPartType, HasSectionType, HasSubsectionType, HasContentType {

    override val least : Long
      get() = content

    override fun toAnchor() : String {
      return "p${part}s${section}ss${subsection}c${content}"
    }

    override fun toString() : String {
      val sb = StringBuilder()
      sb.append(part)
      sb.append(".")
      sb.append(section)
      sb.append(".")
      sb.append(subsection)
      sb.append(".")
      sb.append(content)
      return sb.toString()
    }

    override fun equals(other : Any?) : Boolean {
      if (this === other) return true
      if (other?.javaClass != javaClass) return false
      other as KSNumberPartSectionSubsectionContent
      if (part != other.part) return false
      if (section != other.section) return false
      if (subsection != other.subsection) return false
      if (content != other.content) return false
      return true
    }

    override fun hashCode() : Int {
      var result = part.hashCode()
      result += 31 * result + section.hashCode()
      result += 31 * result + subsection.hashCode()
      result += 31 * result + content.hashCode()
      return result
    }
  }

  class KSNumberSection(
    override val section : Long) : KSNumber(), HasSectionType {

    override val least : Long
      get() = section

    override fun toAnchor() : String {
      return "s${section}"
    }

    override fun toString() : String {
      return section.toString()
    }

    override fun equals(other : Any?) : Boolean {
      if (this === other) return true
      if (other?.javaClass != javaClass) return false
      other as KSNumberSection
      if (section != other.section) return false
      return true
    }

    override fun hashCode() : Int {
      return section.hashCode()
    }
  }

  class KSNumberSectionContent(
    override val section : Long,
    override val content : Long)
  : KSNumber(), HasSectionType, HasContentType {

    override val least : Long
      get() = content

    override fun toAnchor() : String {
      return "s${section}c${content}"
    }

    override fun toString() : String {
      val sb = StringBuilder()
      sb.append(section)
      sb.append(".")
      sb.append(content)
      return sb.toString()
    }

    override fun equals(other : Any?) : Boolean {
      if (this === other) return true
      if (other?.javaClass != javaClass) return false
      other as KSNumberSectionContent
      if (section != other.section) return false
      if (content != other.content) return false
      return true
    }

    override fun hashCode() : Int {
      var result = section.hashCode()
      result += 31 * result + content.hashCode()
      return result
    }
  }

  class KSNumberSectionSubsection(
    override val section : Long,
    override val subsection : Long)
  : KSNumber(), HasSectionType, HasSubsectionType {

    override val least : Long
      get() = subsection

    override fun toAnchor() : String {
      return "s${section}ss${subsection}"
    }

    override fun toString() : String {
      val sb = StringBuilder()
      sb.append(section)
      sb.append(".")
      sb.append(subsection)
      return sb.toString()
    }

    override fun equals(other : Any?) : Boolean {
      if (this === other) return true
      if (other?.javaClass != javaClass) return false
      other as KSNumberSectionSubsection
      if (section != other.section) return false
      if (subsection != other.subsection) return false
      return true
    }

    override fun hashCode() : Int {
      var result = section.hashCode()
      result += 31 * result + subsection.hashCode()
      return result
    }
  }

  class KSNumberSectionSubsectionContent(
    override val section : Long,
    override val subsection : Long,
    override val content : Long)
  : KSNumber(), HasSectionType, HasSubsectionType, HasContentType {

    override val least : Long
      get() = content

    override fun toAnchor() : String {
      return "s${section}ss${subsection}c${content}"
    }

    override fun toString() : String {
      val sb = StringBuilder()
      sb.append(section)
      sb.append(".")
      sb.append(subsection)
      sb.append(".")
      sb.append(content)
      return sb.toString()
    }

    override fun equals(other : Any?) : Boolean {
      if (this === other) return true
      if (other?.javaClass != javaClass) return false
      other as KSNumberSectionSubsectionContent
      if (section != other.section) return false
      if (subsection != other.subsection) return false
      if (content != other.content) return false
      return true
    }

    override fun hashCode() : Int {
      var result = section.hashCode()
      result += 31 * result + subsection.hashCode()
      result += 31 * result + content.hashCode()
      return result
    }
  }
}
