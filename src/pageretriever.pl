#!/usr/bin/perl
# Script allowing automated download of wikipedia articles and categories.
# Arguments :
# 	-d	Specifies the depth of the category tree, defaults to 0 which corresponds to no expansion of the subcategories.
#	-o  Specifies the name of the output file.
#	-v 	Verbose.
#	The rest of the arguments are interpreted as a list of categories to download.
#
use Getopt::Std;
use WWW::Mechanize;
use strict;
use open ':std', ':encoding(UTF-8)';

# ARGUMENT PARSING AND INITIALISATION
my %args;
getopts('d:o:v', \%args);

my $depth = $args{d} || 0;              # Tree depth
my $resultFile = $args{o}               # Output file
	|| ($ENV{'TEMP'} + '/wikimap_raw_default.xml');  

die "You did not mention any categories to download, stopped" if @ARGV == 0;
my @catQueue = @ARGV; 									
my @catBuffer = ();
my %stats = ('expanded' => 0, 'articlenum' => 0);

my $url = 'https://en.wikipedia.org/wiki/Special:Export';
my $mech = WWW::Mechanize->new;
$mech->get($url);
$mech->form_number(1);
		
# WIKIPEDIA EXPORT FORM MANIPULATION
while ($depth >= 0) {
	foreach my $cat (@catQueue){
		$mech->field("catname", $cat);
		$mech->click('addcat');
		$stats{'expanded'}++;
		print "\tExpanded: $cat" if $args{'v'};

		foreach my $line (split(/\n/, $mech->value("pages"))){
			if ($line=~/Category:.++/){
				push(@catBuffer, $line);
			}
			$stats{'articlenum'}++;
		}
	}
	push(@catQueue, filterCats(@catBuffer));
	$depth--;
}

# Filter out categories which will not be expanded
sub filterCats { grep(!/.+?_(stubs|lists)\$/, @_) }

# DUMPING OF THE FILE
open(my $fh, '>', $resultFile);
print $fh $mech->submit->decoded_content;
close $fh;

print "Done downloading $stats{'articlenum'} articles from $stats{'expanded'} Wikipedia categories\n";
