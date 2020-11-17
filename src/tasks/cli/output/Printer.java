package tasks.cli.output;

import omnia.cli.out.Output;
import tasks.cli.command.common.CommonArguments;

public interface Printer {

  Printer print(Output output);

  Printer printLine(Output output);

  interface Factory {
    Printer create(CommonArguments<?> arguments);
  }
}
