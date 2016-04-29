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

  class KSNumberPart(val value : Long) : KSNumber() {

    override fun toString() : String {
      return value.toString()
    }

    override fun equals(other : Any?) : Boolean{
      if (this === other) return true
      if (other?.javaClass != javaClass) return false
      other as KSNumberPart
      if (value != other.value) return false
      return true
    }

    override fun hashCode() : Int{
      return value.hashCode()
    }
  }

  class KSNumberPartSection(
    val part : Long,
    val section : Long) : KSNumber() {

    override fun toString() : String {
      val sb = StringBuilder()
      sb.append(part)
      sb.append(".")
      sb.append(section)
      return sb.toString()
    }

    override fun equals(other : Any?) : Boolean{
      if (this === other) return true
      if (other?.javaClass != javaClass) return false
      other as KSNumberPartSection
      if (part != other.part) return false
      if (section != other.section) return false
      return true
    }

    override fun hashCode() : Int{
      var result = part.hashCode()
      result += 31 * result + section.hashCode()
      return result
    }
  }

  class KSNumberPartSectionContent(
    val part : Long,
    val section : Long,
    val content : Long) : KSNumber() {

    override fun toString() : String {
      val sb = StringBuilder()
      sb.append(part)
      sb.append(".")
      sb.append(section)
      sb.append(".")
      sb.append(content)
      return sb.toString()
    }

    override fun equals(other : Any?) : Boolean{
      if (this === other) return true
      if (other?.javaClass != javaClass) return false
      other as KSNumberPartSectionContent
      if (part != other.part) return false
      if (section != other.section) return false
      if (content != other.content) return false
      return true
    }

    override fun hashCode() : Int{
      var result = part.hashCode()
      result += 31 * result + section.hashCode()
      result += 31 * result + content.hashCode()
      return result
    }
  }

  class KSNumberPartSectionSubsection(
    val part : Long,
    val section : Long,
    val subsection : Long) : KSNumber() {

    override fun toString() : String {
      val sb = StringBuilder()
      sb.append(part)
      sb.append(".")
      sb.append(section)
      sb.append(".")
      sb.append(subsection)
      return sb.toString()
    }

    override fun equals(other : Any?) : Boolean{
      if (this === other) return true
      if (other?.javaClass != javaClass) return false
      other as KSNumberPartSectionSubsection
      if (part != other.part) return false
      if (section != other.section) return false
      if (subsection != other.subsection) return false
      return true
    }

    override fun hashCode() : Int{
      var result = part.hashCode()
      result += 31 * result + section.hashCode()
      result += 31 * result + subsection.hashCode()
      return result
    }
  }

  class KSNumberPartSectionSubsectionContent(
    val part : Long,
    val section : Long,
    val subsection : Long,
    val content : Long) : KSNumber() {

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

    override fun equals(other : Any?) : Boolean{
      if (this === other) return true
      if (other?.javaClass != javaClass) return false
      other as KSNumberPartSectionSubsectionContent
      if (part != other.part) return false
      if (section != other.section) return false
      if (subsection != other.subsection) return false
      if (content != other.content) return false
      return true
    }

    override fun hashCode() : Int{
      var result = part.hashCode()
      result += 31 * result + section.hashCode()
      result += 31 * result + subsection.hashCode()
      result += 31 * result + content.hashCode()
      return result
    }
  }

  class KSNumberSection(val section : Long) : KSNumber() {
    override fun toString() : String {
      return section.toString()
    }

    override fun equals(other : Any?) : Boolean{
      if (this === other) return true
      if (other?.javaClass != javaClass) return false
      other as KSNumberSection
      if (section != other.section) return false
      return true
    }

    override fun hashCode() : Int{
      return section.hashCode()
    }
  }

  class KSNumberSectionContent(
    val section : Long,
    val content : Long) : KSNumber() {

    override fun toString() : String {
      val sb = StringBuilder()
      sb.append(section)
      sb.append(".")
      sb.append(content)
      return sb.toString()
    }

    override fun equals(other : Any?) : Boolean{
      if (this === other) return true
      if (other?.javaClass != javaClass) return false
      other as KSNumberSectionContent
      if (section != other.section) return false
      if (content != other.content) return false
      return true
    }

    override fun hashCode() : Int{
      var result = section.hashCode()
      result += 31 * result + content.hashCode()
      return result
    }
  }

  class KSNumberSectionSubsection(
    val section : Long,
    val subsection : Long) : KSNumber() {

    override fun toString() : String {
      val sb = StringBuilder()
      sb.append(section)
      sb.append(".")
      sb.append(subsection)
      return sb.toString()
    }

    override fun equals(other : Any?) : Boolean{
      if (this === other) return true
      if (other?.javaClass != javaClass) return false
      other as KSNumberSectionSubsection
      if (section != other.section) return false
      if (subsection != other.subsection) return false
      return true
    }

    override fun hashCode() : Int{
      var result = section.hashCode()
      result += 31 * result + subsection.hashCode()
      return result
    }
  }

  class KSNumberSectionSubsectionContent(
    val section : Long,
    val subsection : Long,
    val content : Long) : KSNumber() {

    override fun toString() : String {
      val sb = StringBuilder()
      sb.append(section)
      sb.append(".")
      sb.append(subsection)
      sb.append(".")
      sb.append(content)
      return sb.toString()
    }

    override fun equals(other : Any?) : Boolean{
      if (this === other) return true
      if (other?.javaClass != javaClass) return false
      other as KSNumberSectionSubsectionContent
      if (section != other.section) return false
      if (subsection != other.subsection) return false
      if (content != other.content) return false
      return true
    }

    override fun hashCode() : Int{
      var result = section.hashCode()
      result += 31 * result + subsection.hashCode()
      result += 31 * result + content.hashCode()
      return result
    }
  }
}
