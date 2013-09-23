package org.searchisko.mbox.command;

import org.searchisko.mbox.task.IndexDeltaFolder;
import org.searchisko.mbox.task.IndexMboxArchive;

/**
 * This class is used as a manifest Main-Class. Depending on the first parameter it can execute different actions.
 * <ul>
 *   <li>If the first parameter is "-delta" then delta indexing is started.</li>
 *   <li>Otherwise it fully re-indexed given specified mbox file.</li>
 * </ul>
 *
 * @author Lukáš Vlček (lvlcek@redhat.com)
 */
public class Starter {

    public static void main(String[] args) {

        if (args.length < 1) {
            StringBuilder sb = new StringBuilder();
            sb.append("Invalid parameters!\n");
            sb.append("Usage: Starter [ -delta | other_params ]\n");
            System.out.println(sb.toString());
            return;
        }

        if (args[0].equalsIgnoreCase("-delta")) {
            IndexDeltaFolder.main(args);
        } else {
            IndexMboxArchive.main(args);
        }

    }

}
