#!/bin/sh
exec scala "$0" "$@"
!#

import scala.xml.{Elem, PrettyPrinter, XML}

/** Wiki page, node of the graph. */
class Node(elem: scala.xml.Node) {

  val title: String = (elem \ "title").text

  val url: String = "https://en.wikipedia.org/wiki/" + title.replace(" ", "_")

  val shortTitle: String = title.replace("(programming language)", "")

  val id: Long = (elem \ "id").text.toLong

  val wikitext: String = (elem \ "revision" \ "text").text

  val isProgrammingLanguage: Boolean =
    title.endsWith("(programming language)") || """(?i)\{\{Infobox programming language""".r.findFirstIn(wikitext).isDefined

  val isMeta: Boolean =
    """(?i)(.*list.*|.*comparison.*)""".r.findFirstIn(title).isDefined

  val isCategory: Boolean = title.matches("Category:.*")

  val links: List[String] =
    """\[\[([^#|\]]+)[^\]]*?]]""".r.findAllMatchIn(wikitext).map { m =>
      m.group(1).capitalize.replaceAll("[\\s_]+", " ")
    }.toList
}


case class Graph(nodes: Seq[Node], edges: Map[(Node, Node), Int]) {
  def output(file: Option[String]): Unit = file match {
    case Some(f) => XML.save(f, toGexf, "UTF-8", xmlDecl = true)
    case None => print(new PrettyPrinter(200, 4).format(toGexf))
  }


  lazy val toGexf: scala.xml.Elem =
    <gexf xmlns="http://www.gexf.net/1.2draft"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema−instance"
          xsi:schemaLocation="http://www.gexf.net/1.2draft http://www.gexf.net/1.2draft/gexf.xsd"
          version="1.2">
      <meta lastmodifieddate={java.time.LocalDate.now.toString}>
        <creator>Wikimap (Clément Fournier)</creator>
        <description>Graph of some wikipedia articles</description>
      </meta>
      <graph defaultedgetype="directed">
        <attributes class="node">
          <attribute id="0" title="isProgrammingLanguage" type="boolean"/>
          <attribute id="1" title="isMeta" type="boolean"/>
          <attribute id="2" title="url" type="string"/>
          <attribute id="3" title="isCategory" type="boolean"/>
        </attributes>
        <nodes>
          {nodes map { e =>
          <node id={e.id.toString} label={e.shortTitle}>
            <attvalues>
              <attvalue for="0" value={e.isProgrammingLanguage.toString}/>
              <attvalue for="1" value={e.isMeta.toString}/>
              <attvalue for="2" value={e.url}/>
              <attvalue for="3" value={e.isCategory.toString}/>
            </attvalues>
          </node>
        }}
        </nodes>
        <edges>
          {edges.zipWithIndex.map {
          case (((src, dst), weight), id) => <edge id={id.toString}
                                                   source={src.id.toString}
                                                   target={dst.id.toString}
                                                   weight={weight.toString}/>
        }}
        </edges>
      </graph>
    </gexf>

}


val (input, output) = (args: Array[String]).toList match {
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
} yield new Node(node)



val nodeLookup = Map() ++ pages.map(p => p.title -> p)
val edges = for {
  page <- pages
  link <- page.links
  if nodeLookup.contains(link)
} yield page -> nodeLookup(link)

val weightedEdges = edges.groupBy(identity).mapValues(_.size)

Graph(pages, weightedEdges).output(output)