/**
 *  Copyright (C) 2002-2017   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.networking;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent to update the map generator options.
 */
public class UpdateMapGeneratorOptionsMessage extends ObjectMessage {

    public static final String TAG = "updateMapGeneratorOptions";


    /**
     * Create a new {@code UpdateMapGeneratorOptionsMessage} with the
     * supplied name.
     *
     * @param mapGeneratorOptions The map generator {@code OptionGroup}.
     */
    public UpdateMapGeneratorOptionsMessage(OptionGroup mapGeneratorOptions) {
        super(TAG);

        appendChild(mapGeneratorOptions);
    }

    /**
     * Create a new {@code UpdateMapGeneratorOptionsMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public UpdateMapGeneratorOptionsMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        this(null);

        FreeColXMLReader.ReadScope rs
            = xr.replaceScope(FreeColXMLReader.ReadScope.NOINTERN);
        OptionGroup optionGroup = null;
        try {
            while (xr.moreTags()) {
                String tag = xr.getLocalName();
                if (OptionGroup.TAG.equals(tag)) {
                    if (optionGroup == null) {
                        optionGroup = xr.readFreeColObject(game, OptionGroup.class);
                    } else {
                        expected(TAG, tag);
                    }
                } else {
                    expected(OptionGroup.TAG, tag);
                }
                xr.expectTag(tag);
            }
            xr.expectTag(TAG);
        } finally {
            xr.replaceScope(rs);
        }
        appendChild(optionGroup);
    }


    /**
     * Get the associated option group.
     *
     * @return The options.
     */
    private OptionGroup getMapGeneratorOptions() {
        return getChild(0, OptionGroup.class);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePriority getPriority() {
        return MessagePriority.NORMAL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clientHandler(FreeColClient freeColClient) {
        final Game game = freeColClient.getGame();
        final Specification spec = game.getSpecification();
        final OptionGroup mapOptions = getMapGeneratorOptions();

        if (freeColClient.isInGame()) {
            // Ignore
        } else {
            pgc(freeColClient).updateMapGeneratorOptionsHandler(mapOptions);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        if (serverPlayer == null || !serverPlayer.isAdmin()) {
            return serverPlayer.clientError("Not an admin: " + serverPlayer);
        }
        if (freeColServer.getServerState() != FreeColServer.ServerState.PRE_GAME) {
            return serverPlayer.clientError("Can not change map generator options, "
                + "server state = " + freeColServer.getServerState());
        }
        final Specification spec = freeColServer.getGame().getSpecification();
        final OptionGroup mapOptions = getMapGeneratorOptions();
        if (mapOptions == null) {
            return serverPlayer.clientError("No map generator options to merge");
        }

        return pgc(freeColServer)
            .updateMapGeneratorOptions(serverPlayer, mapOptions);
    }
}
