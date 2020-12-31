package tasks.cli.parser;

import omnia.data.structure.List;

public interface Parser<T> {
  T parse(List<? extends String> commandLine);
}
