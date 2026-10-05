package dev.sobatista.ascent.cloud;

import java.util.List;

/** A provider of compute instances. The autoscaler launches and terminates through this; a local simulator and
 *  (on the roadmap) a real AWS adapter are just two implementations. */
public interface Cloud {
    Worker launch();

    void terminate(Worker worker);

    List<Worker> workers();
}
