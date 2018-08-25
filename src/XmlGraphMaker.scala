#!/bin/sh
exec scala "$0" "$@"
!#

import scala.xml.{Elem, Node, PrettyPrinter, XML}


class Page(elem: Node) {

  def title: String = (elem \ "title").text

  def id: Long = (elem \ "id").text.toLong

  def wikitext: String = (elem \ "revision" \ "text").text

  def links: List[String] =
    """\[\[([^#|\]]+)[^\]]*?]]""".r.findAllMatchIn(wikitext).map { m =>
      m.group(1).capitalize.replaceAll("[\\s_]+", " ")
    }.toList
}

case class Graph(nodes: Map[Long, String], edges: Map[(Long, Long), Int]) {
  def toGexf: scala.xml.Elem =
    <gexf xmlns="http://www.gexf.net/1.2draft"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema−instance"
          xsi:schemaLocation="http://www.gexf.net/1.2draft http://www.gexf.net/1.2draft/gexf.xsd"
          version="1.2">
      <meta lastmodifieddate={java.time.LocalDate.now.toString}>
        <creator>Wikimap (Clément Fournier)</creator>
        <description>Graph of some wikipedia articles</description>
      </meta>
      <graph defaultedgetype="directed">
        <nodes>
          {nodes.map(e => <node id={e._1.toString} label={e._2}/>)}
        </nodes>
        <edges>
          {edges.zipWithIndex.map {
          case (((src, dst), weight), id) => <edge id={id.toString}
                                                   source={src.toString}
                                                   target={dst.toString}
                                                   weight={weight.toString}/>
        }}
        </edges>
      </graph>
    </gexf>

}


val (input, output) = args.toList match {
  case "-i" :: i :: "-o" :: o :: _ => (i, Some(o))
  case "-o" :: o :: "-i" :: i :: _ => (i, Some(o))
  case "-i" :: i :: _ => (i, None)
  case _ => sys.error("Invalid arguments, mention at least the -i flag pls")
}

val doc: Elem = xml.XML.loadFile(input)

val pages = for {
  node <- doc \ "page"
  if !(node \ "title").text.startsWith("Category")
  if (node \ "ns").text.toInt == 0
} yield new Page(node)


val nodeLookup = Map() ++ pages.map(p => p.title -> p.id)
val edges = for {
  page <- pages
  link <- page.links
  if nodeLookup.contains(link)
} yield page.id -> nodeLookup(link)

val weightedEdges = edges.groupBy(identity).mapValues(_.size)
val gexf = Graph(nodeLookup.map(_.swap), weightedEdges).toGexf

if (output.isDefined) XML.save(output.get, gexf, "UTF-8", xmlDecl = true)
else print(new PrettyPrinter(200, 4).format(gexf))

