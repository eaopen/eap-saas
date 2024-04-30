package org.openea.eap.module.saas.service.tenant;

import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.openea.eap.framework.common.enums.CommonStatusEnum;
import org.openea.eap.framework.common.pojo.PageResult;
import org.openea.eap.framework.test.core.ut.BaseDbUnitTest;
import org.openea.eap.framework.test.core.util.AssertUtils;
import org.openea.eap.framework.test.core.util.RandomUtils;
import org.openea.eap.module.saas.controller.admin.tenant.vo.packages.TenantPackagePageReqVO;
import org.openea.eap.module.saas.controller.admin.tenant.vo.packages.TenantPackageSaveReqVO;
import org.openea.eap.module.system.dal.dataobject.tenant.TenantDO;
import org.openea.eap.module.saas.dal.dataobject.tenant.TenantPackageDO;
import org.openea.eap.module.saas.dal.mysql.tenant.TenantPackageMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;
import java.util.List;

import static org.openea.eap.framework.common.util.date.LocalDateTimeUtils.buildBetweenTime;
import static org.openea.eap.framework.common.util.date.LocalDateTimeUtils.buildTime;
import static org.openea.eap.framework.common.util.object.ObjectUtils.cloneIgnoreId;
import static org.openea.eap.framework.test.core.util.AssertUtils.assertPojoEquals;
import static org.openea.eap.framework.test.core.util.AssertUtils.assertServiceException;
import static org.openea.eap.module.system.enums.ErrorCodeConstants.*;
import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
* {@link TenantPackageServiceImpl} 的单元测试类
*
*/
@Import(TenantPackageServiceImpl.class)
public class TenantPackageServiceImplTest extends BaseDbUnitTest {

    @Resource
    private TenantPackageServiceImpl tenantPackageService;

    @Resource
    private TenantPackageMapper tenantPackageMapper;

    @MockBean
    private TenantServiceExt tenantService;

    @Test
    public void testCreateTenantPackage_success() {
        // 准备参数
        TenantPackageSaveReqVO reqVO = RandomUtils.randomPojo(TenantPackageSaveReqVO.class,
                o -> o.setStatus(RandomUtils.randomCommonStatus()))
                .setId(null); // 防止 id 被赋值

        // 调用
        Long tenantPackageId = tenantPackageService.createTenantPackage(reqVO);
        // 断言
        Assertions.assertNotNull(tenantPackageId);
        // 校验记录的属性是否正确
        TenantPackageDO tenantPackage = tenantPackageMapper.selectById(tenantPackageId);
        assertPojoEquals(reqVO, tenantPackage, "id");
    }

    @Test
    public void testUpdateTenantPackage_success() {
        // mock 数据
        TenantPackageDO dbTenantPackage = RandomUtils.randomPojo(TenantPackageDO.class,
                o -> o.setStatus(RandomUtils.randomCommonStatus()));
        tenantPackageMapper.insert(dbTenantPackage);// @Sql: 先插入出一条存在的数据
        // 准备参数
        TenantPackageSaveReqVO reqVO = RandomUtils.randomPojo(TenantPackageSaveReqVO.class, o -> {
            o.setId(dbTenantPackage.getId()); // 设置更新的 ID
            o.setStatus(RandomUtils.randomCommonStatus());
        });
        // mock 方法
        Long tenantId01 = RandomUtils.randomLongId();
        Long tenantId02 = RandomUtils.randomLongId();
        Mockito.when(tenantService.getTenantListByPackageId(eq(reqVO.getId()))).thenReturn(
                asList(RandomUtils.randomPojo(TenantDO.class, o -> o.setId(tenantId01)),
                        RandomUtils.randomPojo(TenantDO.class, o -> o.setId(tenantId02))));

        // 调用
        tenantPackageService.updateTenantPackage(reqVO);
        // 校验是否更新正确
        TenantPackageDO tenantPackage = tenantPackageMapper.selectById(reqVO.getId()); // 获取最新的
        AssertUtils.assertPojoEquals(reqVO, tenantPackage);
        // 校验调用租户的菜单
        Mockito.verify(tenantService).updateTenantRoleMenu(ArgumentMatchers.eq(tenantId01), eq(reqVO.getMenuIds()));
        Mockito.verify(tenantService).updateTenantRoleMenu(ArgumentMatchers.eq(tenantId02), eq(reqVO.getMenuIds()));
    }

    @Test
    public void testUpdateTenantPackage_notExists() {
        // 准备参数
        TenantPackageSaveReqVO reqVO = RandomUtils.randomPojo(TenantPackageSaveReqVO.class);

        // 调用, 并断言异常
        AssertUtils.assertServiceException(() -> tenantPackageService.updateTenantPackage(reqVO), TENANT_PACKAGE_NOT_EXISTS);
    }

    @Test
    public void testDeleteTenantPackage_success() {
        // mock 数据
        TenantPackageDO dbTenantPackage = RandomUtils.randomPojo(TenantPackageDO.class);
        tenantPackageMapper.insert(dbTenantPackage);// @Sql: 先插入出一条存在的数据
        // 准备参数
        Long id = dbTenantPackage.getId();
        // mock 租户未使用该套餐
        Mockito.when(tenantService.getTenantCountByPackageId(ArgumentMatchers.eq(id))).thenReturn(0L);

        // 调用
        tenantPackageService.deleteTenantPackage(id);
       // 校验数据不存在了
       Assertions.assertNull(tenantPackageMapper.selectById(id));
    }

    @Test
    public void testDeleteTenantPackage_notExists() {
        // 准备参数
        Long id = RandomUtils.randomLongId();

        // 调用, 并断言异常
        AssertUtils.assertServiceException(() -> tenantPackageService.deleteTenantPackage(id), TENANT_PACKAGE_NOT_EXISTS);
    }

    @Test
    public void testDeleteTenantPackage_used() {
        // mock 数据
        TenantPackageDO dbTenantPackage = RandomUtils.randomPojo(TenantPackageDO.class);
        tenantPackageMapper.insert(dbTenantPackage);// @Sql: 先插入出一条存在的数据
        // 准备参数
        Long id = dbTenantPackage.getId();
        // mock 租户在使用该套餐
        Mockito.when(tenantService.getTenantCountByPackageId(ArgumentMatchers.eq(id))).thenReturn(1L);

        // 调用, 并断言异常
        AssertUtils.assertServiceException(() -> tenantPackageService.deleteTenantPackage(id), TENANT_PACKAGE_USED);
    }

    @Test
    public void testGetTenantPackagePage() {
       // mock 数据
       TenantPackageDO dbTenantPackage = RandomUtils.randomPojo(TenantPackageDO.class, o -> { // 等会查询到
           o.setName("OpenEAP");
           o.setStatus(CommonStatusEnum.ENABLE.getStatus());
           o.setRemark("源码解析");
           o.setCreateTime(buildTime(2022, 10, 10));
       });
       tenantPackageMapper.insert(dbTenantPackage);
       // 测试 name 不匹配
       tenantPackageMapper.insert(cloneIgnoreId(dbTenantPackage, o -> o.setName("源码")));
       // 测试 status 不匹配
       tenantPackageMapper.insert(cloneIgnoreId(dbTenantPackage, o -> o.setStatus(CommonStatusEnum.DISABLE.getStatus())));
       // 测试 remark 不匹配
       tenantPackageMapper.insert(cloneIgnoreId(dbTenantPackage, o -> o.setRemark("解析")));
       // 测试 createTime 不匹配
       tenantPackageMapper.insert(cloneIgnoreId(dbTenantPackage, o -> o.setCreateTime(buildTime(2022, 11, 11))));
       // 准备参数
       TenantPackagePageReqVO reqVO = new TenantPackagePageReqVO();
       reqVO.setName("芋道");
       reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
       reqVO.setRemark("源码");
       reqVO.setCreateTime(buildBetweenTime(2022, 10, 9, 2022, 10, 11));

       // 调用
       PageResult<TenantPackageDO> pageResult = tenantPackageService.getTenantPackagePage(reqVO);
       // 断言
       Assertions.assertEquals(1, pageResult.getTotal());
       Assertions.assertEquals(1, pageResult.getList().size());
       AssertUtils.assertPojoEquals(dbTenantPackage, pageResult.getList().get(0));
    }

    @Test
    public void testValidTenantPackage_success() {
        // mock 数据
        TenantPackageDO dbTenantPackage = RandomUtils.randomPojo(TenantPackageDO.class,
                o -> o.setStatus(CommonStatusEnum.ENABLE.getStatus()));
        tenantPackageMapper.insert(dbTenantPackage);// @Sql: 先插入出一条存在的数据

        // 调用
        TenantPackageDO result = tenantPackageService.validTenantPackage(dbTenantPackage.getId());
        // 断言
        AssertUtils.assertPojoEquals(dbTenantPackage, result);
    }

    @Test
    public void testValidTenantPackage_notExists() {
        // 准备参数
        Long id = RandomUtils.randomLongId();

        // 调用, 并断言异常
        AssertUtils.assertServiceException(() -> tenantPackageService.validTenantPackage(id), TENANT_PACKAGE_NOT_EXISTS);
    }

    @Test
    public void testValidTenantPackage_disable() {
        // mock 数据
        TenantPackageDO dbTenantPackage = RandomUtils.randomPojo(TenantPackageDO.class,
                o -> o.setStatus(CommonStatusEnum.DISABLE.getStatus()));
        tenantPackageMapper.insert(dbTenantPackage);// @Sql: 先插入出一条存在的数据

        // 调用, 并断言异常
        assertServiceException(() -> tenantPackageService.validTenantPackage(dbTenantPackage.getId()),
                TENANT_PACKAGE_DISABLE, dbTenantPackage.getName());
    }

    @Test
    public void testGetTenantPackage() {
        // mock 数据
        TenantPackageDO dbTenantPackage = RandomUtils.randomPojo(TenantPackageDO.class);
        tenantPackageMapper.insert(dbTenantPackage);// @Sql: 先插入出一条存在的数据

        // 调用
        TenantPackageDO result = tenantPackageService.getTenantPackage(dbTenantPackage.getId());
        // 断言
        AssertUtils.assertPojoEquals(result, dbTenantPackage);
    }

    @Test
    public void testGetTenantPackageListByStatus() {
        // mock 数据
        TenantPackageDO dbTenantPackage = RandomUtils.randomPojo(TenantPackageDO.class,
                o -> o.setStatus(CommonStatusEnum.ENABLE.getStatus()));
        tenantPackageMapper.insert(dbTenantPackage);
        // 测试 status 不匹配
        tenantPackageMapper.insert(cloneIgnoreId(dbTenantPackage,
                o -> o.setStatus(CommonStatusEnum.DISABLE.getStatus())));

        // 调用
        List<TenantPackageDO> list = tenantPackageService.getTenantPackageListByStatus(
                CommonStatusEnum.ENABLE.getStatus());
        Assertions.assertEquals(1, list.size());
        AssertUtils.assertPojoEquals(dbTenantPackage, list.get(0));
    }

}
