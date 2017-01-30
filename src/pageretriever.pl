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
use Set::Scalar;
use Set::Light;
use strict;
use open ':std', ':encoding(UTF-8)';

# ARGUMENT PARSING AND INITIALISATION
my %args;
getopts('d:o:v', \%args);

my $depth = $args{d} || 0;              # Tree depth
my $resultFile = $args{o}               # Output file
	|| ($ENV{'TEMP'} + '/wikimap_raw_default.xml');  

die "You did not mention any categories to download, stopped" if @ARGV == 0;
my @catQueue = @ARGV; 					# Categories left to process
my @catBuffer = ();						# Buffs the cats to expand at next depth 
my $processedCats = Set::Light->new();	# Stores processed categories
my $articleSet = Set::Scalar->new();	# Stores processed articles

my $url = 'https://en.wikipedia.org/wiki/Special:Export';
my $mech = WWW::Mechanize->new;
$mech->get($url);
$mech->form_number(1);
		
# WIKIPEDIA EXPORT FORM MANIPULATION
while ($depth >= 0) {
	foreach my $cat (@catQueue){
		if (!$processedCats->has($cat)){
			$mech->field("catname", $cat);
			$mech->click('addcat');

			print "\tExpanded: $cat\n" if $args{'v'};

			foreach my $line (split(/\n/, $mech->value("pages"))){
				if ($line=~/Category:.++/){
					push(@catBuffer, $line);
				} else {
					$articleSet->insert($line);
				}
			}
			$processedCats->insert($cat);
			$mech->set_fields( "pages" => "");
		}
	}
	push(@catQueue, filterCats(@catBuffer));
	$depth--;
}

# Filter out categories which will not be expanded
sub filterCats { grep(!/.+?_(stubs|lists)\$/, @_) }

my $allpages = $articleSet;
$allpages =~ s/ /\n/g;
$allpages = substr $allpages, 1, length($allpages) - 2;
$mech->set_fields( "pages" => $allpages);

# DUMPING OF THE FILE
open(my $fh, '>', $resultFile);
print $fh $mech->submit->decoded_content;
close $fh;

print "Done downloading " . $articleSet->size() . " articles from " . $processedCats->size() . " Wikipedia categories\n";