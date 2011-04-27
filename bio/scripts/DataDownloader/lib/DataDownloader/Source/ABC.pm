package DataDownloader::Source::ABC;

use Moose;
use MooseX::ABC;
use MooseX::FollowPBP;

require DataDownloader::Resource::HTTP;
require DataDownloader::Resource::FTP;

with 'DataDownloader::Role::Source';
with 'DataDownloader::Role::SystemCommand';

use feature 'switch';

# The default download method
use constant METHOD => "HTTP";

sub BUILD {
    my $self = shift;
    if ($self->can('SOURCES')) {
        $self->set_sources($self->SOURCES);
    }
}

has sources => (
    is => 'rw',
    isa => 'ArrayRef[HashRef]',
    auto_deref => 1,
);

sub get_all_sources {
    my $self = shift;
    my @sources = map {$self->make_source($_)} 
                  map {$self->adjust_source_arguments($_)}
                    $self->get_sources;
    return @sources;
}

=head2 adjust_source_arguments

By default nothing is done to the source hash-refs. If these should be altered, 
then override this method to filter them.

=cut

sub adjust_source_arguments {
    my $self = shift;
    my $args = shift;
    return $args;
}

=head2 make_source 

makes HTTP and FTP sources from source arguments. Sources automatically
inherit logger and title attributes unless specified.

HTTP sources are the default

=cut

sub make_source {
    my $self = shift;
    my $args = shift;;

    $args->{logger}   ||= $self->get_logger;
    $args->{title}    ||= $self->get_title;
    $args->{main_dir} ||= $self->get_destination_dir;

    my $method = delete($args->{METHOD}) || $self->METHOD;
    given (lc($method)) {
        when("http") {return DataDownloader::Resource::HTTP->new($args)}
        when("ftp")  {return DataDownloader::Resource::FTP->new($args)}
        default {confess "No source type found for method: $method"}
    }
}

sub fetch_all_data {
    my $self = shift;
    for my $source ($self->get_all_sources) {
        $source->fetch();
    }
}

1;