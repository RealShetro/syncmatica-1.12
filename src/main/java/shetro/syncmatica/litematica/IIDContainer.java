package shetro.syncmatica.litematica;

import java.util.UUID;

public interface IIDContainer {
	void setServerId(UUID i);

	UUID getServerId();
}