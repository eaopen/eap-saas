package org.openea.eap.module.saas.service.tenant;

import org.openea.eap.module.system.controller.admin.tenant.vo.tenant.TenantSaveReqVO;
import org.openea.eap.module.system.dal.dataobject.tenant.TenantDO;
import org.openea.eap.module.system.service.tenant.TenantService;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

/**
 * 租户 Service 接口
 *
 */
public interface TenantServiceExt extends TenantService {



    /**
     * 创建租户
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createTenant(@Valid TenantSaveReqVO createReqVO);

    /**
     * 更新租户
     *
     * @param updateReqVO 更新信息
     */
    void updateTenant(@Valid TenantSaveReqVO updateReqVO);

    /**
     * 更新租户的角色菜单
     *
     * @param tenantId 租户编号
     * @param menuIds 菜单编号数组
     */
    void updateTenantRoleMenu(Long tenantId, Set<Long> menuIds);

    /**
     * 删除租户
     *
     * @param id 编号
     */
    void deleteTenant(Long id);




    /**
     * 获得使用指定套餐的租户数量
     *
     * @param packageId 租户套餐编号
     * @return 租户数量
     */
    Long getTenantCountByPackageId(Long packageId);

    /**
     * 获得使用指定套餐的租户数组
     *
     * @param packageId 租户套餐编号
     * @return 租户数组
     */
    List<TenantDO> getTenantListByPackageId(Long packageId);


}
