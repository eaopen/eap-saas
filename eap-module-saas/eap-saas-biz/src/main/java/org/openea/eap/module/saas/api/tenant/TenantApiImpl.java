package org.openea.eap.module.saas.api.tenant;

import org.openea.eap.module.saas.service.tenant.TenantService2;
import org.openea.eap.module.system.api.tenant.TenantApi;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 多租户的 API 实现类
 *
 */
@Service
public class TenantApiImpl implements TenantApi {

    @Resource
    private TenantService2 tenantService;

    @Override
    public List<Long> getTenantIdList() {
        return tenantService.getTenantIdList();
    }

    @Override
    public void validateTenant(Long id) {
        tenantService.validTenant(id);
    }

}
