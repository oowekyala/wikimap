# Wikimap : wikipedia cartography pre-processing tool

This program allows the user to easily **download and preprocess a set of articles from Wikipedia**, e.g. a category, with or without its subcategories. The program **outputs CSV files** which describe the graph (that is, its nodes and edges), which can then be plotted using free graph visualisation software such as [Gephi](https://gephi.org/ "Gephi's homepage").

## Gallery

The following images are snapshots of the graph obtained with several linguistics-related categories. 

Full graph.
![alt text][full-linguistics]
Snapshot : upper right blue cluster.
![alt text][phonology]
Snapshot : middle right black cluster.
![alt text][sociolinguistics]
Snapshot : middle left orange cluster.
![alt text][formal_languages]

## Design

The program is made up of several scripts, which:

1. *Download* a set of articles in the wikipedia XML flavour
2. *Parse* this XML file into another XML file which contains only relevant information (article titles, links and category information)
3. Load this data into an embedded database (using [Apache Derby](https://db.apache.org/derby/papers/DerbyTut/embedded_intro.html "Embedded Derby"))
4. *Process the data in the database* so as to, for example, retain only links to other articles of the subset, weigh links, eliminate redirection pages (while still counting links to redirections as links to articles)...
5. Finally dump the database into two CSV files, one for the edges, one for the nodes

[phonology]: ./images/phonology.png "Detail : phonology"
[sociolinguistics]: ./images/sociolinguistics.png "Detail : sociolinguistics"
[formal_languages]: ./images/formal_languages.png "Detail : formal languages"
[full-linguistics]: ./images/full-linguistics.png "Full subset graph, with articles from linguistics-related categories"
