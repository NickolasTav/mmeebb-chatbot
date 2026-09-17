package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.dto.NgrokTunnelDto;

public interface NgrokTunnelService {

    NgrokTunnelDto getTunnelStatus();

    NgrokTunnelDto discoverTunnel();

    boolean isTunnelOnline();
}
