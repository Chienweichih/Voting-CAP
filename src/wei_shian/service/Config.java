package wei_shian.service;

/**
 *
 * @author chienweichih
 */
public interface Config extends wei_chih.service.Config {
    public int WEI_SHIAN_SERVICE_PORT = SERVICE_PORT[SERVICE_PORT.length - 1];
    public int WEI_SHIAN_SYNC_PORT = SERVICE_PORT[SERVICE_PORT.length - 2];
}