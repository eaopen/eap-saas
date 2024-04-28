package org.openea.eap.module.saas.service.tenant;

import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.openea.eap.framework.common.enums.CommonStatusEnum;
import org.openea.eap.framework.common.pojo.PageResult;
import org.openea.eap.framework.tenant.config.TenantProperties;
import org.openea.eap.framework.tenant.core.context.TenantContextHolder;
import org.openea.eap.framework.test.core.ut.BaseDbUnitTest;
import org.openea.eap.framework.test.core.util.AssertUtils;
import org.openea.eap.framework.test.core.util.RandomUtils;
import org.openea.eap.module.system.controller.admin.tenant.vo.tenant.TenantPageReqVO;
import org.openea.eap.module.saas.controller.admin.tenant.vo.tenant.TenantSaveReqVO;
import org.openea.eap.module.system.dal.dataobject.permission.MenuDO;
import org.openea.eap.module.system.dal.dataobject.permission.RoleDO;
import org.openea.eap.module.system.dal.dataobject.tenant.TenantDO;
import org.openea.eap.module.saas.dal.dataobject.tenant.TenantPackageDO;
import org.openea.eap.module.system.dal.mysql.tenant.TenantMapper;
import org.openea.eap.module.system.enums.permission.RoleCodeEnum;
import org.openea.eap.module.system.enums.permission.RoleTypeEnum;
import org.openea.eap.module.system.service.permission.MenuService;
import org.openea.eap.module.system.service.permission.PermissionService;
import org.openea.eap.module.system.service.permission.RoleService;
import org.openea.eap.module.system.service.tenant.handler.TenantInfoHandler;
import org.openea.eap.module.system.service.tenant.handler.TenantMenuHandler;
import org.openea.eap.module.system.service.user.AdminUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.openea.eap.framework.common.util.collection.SetUtils.asSet;
import static org.openea.eap.framework.common.util.date.LocalDateTimeUtils.buildBetweenTime;
import static org.openea.eap.framework.common.util.date.LocalDateTimeUtils.buildTime;
import static org.openea.eap.framework.common.util.object.ObjectUtils.cloneIgnoreId;
import static org.openea.eap.framework.test.core.util.AssertUtils.assertPojoEquals;
import static org.openea.eap.module.system.dal.dataobject.tenant.TenantDO.PACKAGE_ID_SYSTEM;
import static org.openea.eap.module.system.enums.ErrorCodeConstants.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.mockito.ArgumentMatchers.*;

/**
 * {@link TenantServiceImpl} 的单元测试类
 *
 */
@Import(TenantServiceImpl.class)
public class TenantServiceImplTest extends BaseDbUnitTest {

    @Resource
    private TenantServiceImpl tenantService;

    @Resource
    private TenantMapper tenantMapper;

    @MockBean
    private TenantProperties tenantProperties;
    @MockBean
    private TenantPackageService tenantPackageService;
    @MockBean
    private AdminUserService userService;
    @MockBean
    private RoleService roleService;
    @MockBean
    private MenuService menuService;
    @MockBean
    private PermissionService permissionService;

    @BeforeEach
    public void setUp() {
        // 清理租户上下文
        TenantContextHolder.clear();
    }

    @Test
    public void testGetTenantIdList() {
        // mock 数据
        TenantDO tenant = RandomUtils.randomPojo(TenantDO.class, o -> o.setId(1L));
        tenantMapper.insert(tenant);

        // 调用，并断言业务异常
        List<Long> result = tenantService.getTenantIdList();
        Assertions.assertEquals(Collections.singletonList(1L), result);
    }

    @Test
    public void testValidTenant_notExists() {
        AssertUtils.assertServiceException(() -> tenantService.validTenant(RandomUtils.randomLongId()), TENANT_NOT_EXISTS);
    }

    @Test
    public void testValidTenant_disable() {
        // mock 数据
        TenantDO tenant = RandomUtils.randomPojo(TenantDO.class, o -> o.setId(1L).setStatus(CommonStatusEnum.DISABLE.getStatus()));
        tenantMapper.insert(tenant);

        // 调用，并断言业务异常
        AssertUtils.assertServiceException(() -> tenantService.validTenant(1L), TENANT_DISABLE, tenant.getName());
    }

    @Test
    public void testValidTenant_expired() {
        // mock 数据
        TenantDO tenant = RandomUtils.randomPojo(TenantDO.class, o -> o.setId(1L).setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setExpireTime(buildTime(2020, 2, 2)));
        tenantMapper.insert(tenant);

        // 调用，并断言业务异常
        AssertUtils.assertServiceException(() -> tenantService.validTenant(1L), TENANT_EXPIRE, tenant.getName());
    }

    @Test
    public void testValidTenant_success() {
        // mock 数据
        TenantDO tenant = RandomUtils.randomPojo(TenantDO.class, o -> o.setId(1L).setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setExpireTime(LocalDateTime.now().plusDays(1)));
        tenantMapper.insert(tenant);

        // 调用，并断言业务异常
        tenantService.validTenant(1L);
    }

    @Test
    public void testCreateTenant() {
        // mock 套餐 100L
        TenantPackageDO tenantPackage = RandomUtils.randomPojo(TenantPackageDO.class, o -> o.setId(100L));
        Mockito.when(tenantPackageService.validTenantPackage(ArgumentMatchers.eq(100L))).thenReturn(tenantPackage);
        // mock 角色 200L
        Mockito.when(roleService.createRole(ArgumentMatchers.argThat(role -> {
            Assertions.assertEquals(RoleCodeEnum.TENANT_ADMIN.getName(), role.getName());
            Assertions.assertEquals(RoleCodeEnum.TENANT_ADMIN.getCode(), role.getCode());
            Assertions.assertEquals(0, role.getSort());
            Assertions.assertEquals("系统自动生成", role.getRemark());
            return true;
        }), ArgumentMatchers.eq(RoleTypeEnum.SYSTEM.getType()))).thenReturn(200L);
        // mock 用户 300L
        Mockito.when(userService.createUser(ArgumentMatchers.argThat(user -> {
            Assertions.assertEquals("yunai", user.getUsername());
            Assertions.assertEquals("yuanma", user.getPassword());
            Assertions.assertEquals("芋道", user.getNickname());
            Assertions.assertEquals("15601691300", user.getMobile());
            return true;
        }))).thenReturn(300L);

        // 准备参数
        TenantSaveReqVO reqVO = RandomUtils.randomPojo(TenantSaveReqVO.class, o -> {
            o.setContactName("芋道");
            o.setContactMobile("15601691300");
            o.setPackageId(100L);
            o.setStatus(RandomUtils.randomCommonStatus());
            o.setWebsite("https://www.iocoder.cn");
            o.setUsername("yunai");
            o.setPassword("yuanma");
        }).setId(null); // 设置为 null，方便后面校验

        // 调用
        Long tenantId = tenantService.createTenant(reqVO);
        // 断言
        Assertions.assertNotNull(tenantId);
        // 校验记录的属性是否正确
        TenantDO tenant = tenantMapper.selectById(tenantId);
        assertPojoEquals(reqVO, tenant, "id");
        Assertions.assertEquals(300L, tenant.getContactUserId());
        // verify 分配权限
        Mockito.verify(permissionService).assignRoleMenu(ArgumentMatchers.eq(200L), ArgumentMatchers.same(tenantPackage.getMenuIds()));
        // verify 分配角色
        Mockito.verify(permissionService).assignUserRole(ArgumentMatchers.eq(300L), ArgumentMatchers.eq(singleton(200L)));
    }

    @Test
    public void testUpdateTenant_success() {
        // mock 数据
        TenantDO dbTenant = RandomUtils.randomPojo(TenantDO.class, o -> o.setStatus(RandomUtils.randomCommonStatus()));
        tenantMapper.insert(dbTenant);// @Sql: 先插入出一条存在的数据
        // 准备参数
        TenantSaveReqVO reqVO = RandomUtils.randomPojo(TenantSaveReqVO.class, o -> {
            o.setId(dbTenant.getId()); // 设置更新的 ID
            o.setStatus(RandomUtils.randomCommonStatus());
            o.setWebsite(RandomUtils.randomString());
        });

        // mock 套餐
        TenantPackageDO tenantPackage = RandomUtils.randomPojo(TenantPackageDO.class,
                o -> o.setMenuIds(asSet(200L, 201L)));
        Mockito.when(tenantPackageService.validTenantPackage(eq(reqVO.getPackageId()))).thenReturn(tenantPackage);
        // mock 所有角色
        RoleDO role100 = RandomUtils.randomPojo(RoleDO.class, o -> o.setId(100L).setCode(RoleCodeEnum.TENANT_ADMIN.getCode()));
        role100.setTenantId(dbTenant.getId());
        RoleDO role101 = RandomUtils.randomPojo(RoleDO.class, o -> o.setId(101L));
        role101.setTenantId(dbTenant.getId());
        Mockito.when(roleService.getRoleList()).thenReturn(asList(role100, role101));
        // mock 每个角色的权限
        Mockito.when(permissionService.getRoleMenuListByRoleId(ArgumentMatchers.eq(101L))).thenReturn(asSet(201L, 202L));

        // 调用
        tenantService.updateTenant(reqVO);
        // 校验是否更新正确
        TenantDO tenant = tenantMapper.selectById(reqVO.getId()); // 获取最新的
        AssertUtils.assertPojoEquals(reqVO, tenant);
        // verify 设置角色权限
        Mockito.verify(permissionService).assignRoleMenu(ArgumentMatchers.eq(100L), ArgumentMatchers.eq(asSet(200L, 201L)));
        Mockito.verify(permissionService).assignRoleMenu(ArgumentMatchers.eq(101L), ArgumentMatchers.eq(asSet(201L)));
    }

    @Test
    public void testUpdateTenant_notExists() {
        // 准备参数
        TenantSaveReqVO reqVO = RandomUtils.randomPojo(TenantSaveReqVO.class);

        // 调用, 并断言异常
        AssertUtils.assertServiceException(() -> tenantService.updateTenant(reqVO), TENANT_NOT_EXISTS);
    }

    @Test
    public void testUpdateTenant_system() {
        // mock 数据
        TenantDO dbTenant = RandomUtils.randomPojo(TenantDO.class, o -> o.setPackageId(PACKAGE_ID_SYSTEM));
        tenantMapper.insert(dbTenant);// @Sql: 先插入出一条存在的数据
        // 准备参数
        TenantSaveReqVO reqVO = RandomUtils.randomPojo(TenantSaveReqVO.class, o -> {
            o.setId(dbTenant.getId()); // 设置更新的 ID
        });

        // 调用，校验业务异常
        AssertUtils.assertServiceException(() -> tenantService.updateTenant(reqVO), TENANT_CAN_NOT_UPDATE_SYSTEM);
    }

    @Test
    public void testDeleteTenant_success() {
        // mock 数据
        TenantDO dbTenant = RandomUtils.randomPojo(TenantDO.class,
                o -> o.setStatus(RandomUtils.randomCommonStatus()));
        tenantMapper.insert(dbTenant);// @Sql: 先插入出一条存在的数据
        // 准备参数
        Long id = dbTenant.getId();

        // 调用
        tenantService.deleteTenant(id);
        // 校验数据不存在了
        Assertions.assertNull(tenantMapper.selectById(id));
    }

    @Test
    public void testDeleteTenant_notExists() {
        // 准备参数
        Long id = RandomUtils.randomLongId();

        // 调用, 并断言异常
        AssertUtils.assertServiceException(() -> tenantService.deleteTenant(id), TENANT_NOT_EXISTS);
    }

    @Test
    public void testDeleteTenant_system() {
        // mock 数据
        TenantDO dbTenant = RandomUtils.randomPojo(TenantDO.class, o -> o.setPackageId(PACKAGE_ID_SYSTEM));
        tenantMapper.insert(dbTenant);// @Sql: 先插入出一条存在的数据
        // 准备参数
        Long id = dbTenant.getId();

        // 调用, 并断言异常
        AssertUtils.assertServiceException(() -> tenantService.deleteTenant(id), TENANT_CAN_NOT_UPDATE_SYSTEM);
    }

    @Test
    public void testGetTenant() {
        // mock 数据
        TenantDO dbTenant = RandomUtils.randomPojo(TenantDO.class);
        tenantMapper.insert(dbTenant);// @Sql: 先插入出一条存在的数据
        // 准备参数
        Long id = dbTenant.getId();

        // 调用
        TenantDO result = tenantService.getTenant(id);
        // 校验存在
        AssertUtils.assertPojoEquals(result, dbTenant);
    }

    @Test
    public void testGetTenantPage() {
        // mock 数据
        TenantDO dbTenant = RandomUtils.randomPojo(TenantDO.class, o -> { // 等会查询到
            o.setName("芋道源码");
            o.setContactName("芋艿");
            o.setContactMobile("15601691300");
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
            o.setCreateTime(buildTime(2020, 12, 12));
        });
        tenantMapper.insert(dbTenant);
        // 测试 name 不匹配
        tenantMapper.insert(cloneIgnoreId(dbTenant, o -> o.setName(RandomUtils.randomString())));
        // 测试 contactName 不匹配
        tenantMapper.insert(cloneIgnoreId(dbTenant, o -> o.setContactName(RandomUtils.randomString())));
        // 测试 contactMobile 不匹配
        tenantMapper.insert(cloneIgnoreId(dbTenant, o -> o.setContactMobile(RandomUtils.randomString())));
        // 测试 status 不匹配
        tenantMapper.insert(cloneIgnoreId(dbTenant, o -> o.setStatus(CommonStatusEnum.DISABLE.getStatus())));
        // 测试 createTime 不匹配
        tenantMapper.insert(cloneIgnoreId(dbTenant, o -> o.setCreateTime(buildTime(2021, 12, 12))));
        // 准备参数
        TenantPageReqVO reqVO = new TenantPageReqVO();
        reqVO.setName("芋道");
        reqVO.setContactName("艿");
        reqVO.setContactMobile("1560");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        reqVO.setCreateTime(buildBetweenTime(2020, 12, 1, 2020, 12, 24));

        // 调用
        PageResult<TenantDO> pageResult = tenantService.getTenantPage(reqVO);
        // 断言
        Assertions.assertEquals(1, pageResult.getTotal());
        Assertions.assertEquals(1, pageResult.getList().size());
        AssertUtils.assertPojoEquals(dbTenant, pageResult.getList().get(0));
    }

    @Test
    public void testGetTenantByName() {
        // mock 数据
        TenantDO dbTenant = RandomUtils.randomPojo(TenantDO.class, o -> o.setName("芋道"));
        tenantMapper.insert(dbTenant);// @Sql: 先插入出一条存在的数据

        // 调用
        TenantDO result = tenantService.getTenantByName("芋道");
        // 校验存在
        AssertUtils.assertPojoEquals(result, dbTenant);
    }

    @Test
    public void testGetTenantByWebsite() {
        // mock 数据
        TenantDO dbTenant = RandomUtils.randomPojo(TenantDO.class, o -> o.setWebsite("https://www.iocoder.cn"));
        tenantMapper.insert(dbTenant);// @Sql: 先插入出一条存在的数据

        // 调用
        TenantDO result = tenantService.getTenantByWebsite("https://www.iocoder.cn");
        // 校验存在
        AssertUtils.assertPojoEquals(result, dbTenant);
    }

    @Test
    public void testGetTenantListByPackageId() {
        // mock 数据
        TenantDO dbTenant1 = RandomUtils.randomPojo(TenantDO.class, o -> o.setPackageId(1L));
        tenantMapper.insert(dbTenant1);// @Sql: 先插入出一条存在的数据
        TenantDO dbTenant2 = RandomUtils.randomPojo(TenantDO.class, o -> o.setPackageId(2L));
        tenantMapper.insert(dbTenant2);// @Sql: 先插入出一条存在的数据

        // 调用
        List<TenantDO> result = tenantService.getTenantListByPackageId(1L);
        Assertions.assertEquals(1, result.size());
        AssertUtils.assertPojoEquals(dbTenant1, result.get(0));
    }

    @Test
    public void testGetTenantCountByPackageId() {
        // mock 数据
        TenantDO dbTenant1 = RandomUtils.randomPojo(TenantDO.class, o -> o.setPackageId(1L));
        tenantMapper.insert(dbTenant1);// @Sql: 先插入出一条存在的数据
        TenantDO dbTenant2 = RandomUtils.randomPojo(TenantDO.class, o -> o.setPackageId(2L));
        tenantMapper.insert(dbTenant2);// @Sql: 先插入出一条存在的数据

        // 调用
        Long count = tenantService.getTenantCountByPackageId(1L);
        Assertions.assertEquals(1, count);
    }

    @Test
    public void testHandleTenantInfo_disable() {
        // 准备参数
        TenantInfoHandler handler = Mockito.mock(TenantInfoHandler.class);
        // mock 禁用
        Mockito.when(tenantProperties.getEnable()).thenReturn(false);

        // 调用
        tenantService.handleTenantInfo(handler);
        // 断言
        Mockito.verify(handler, Mockito.never()).handle(ArgumentMatchers.any());
    }

    @Test
    public void testHandleTenantInfo_success() {
        // 准备参数
        TenantInfoHandler handler = Mockito.mock(TenantInfoHandler.class);
        // mock 未禁用
        Mockito.when(tenantProperties.getEnable()).thenReturn(true);
        // mock 租户
        TenantDO dbTenant = RandomUtils.randomPojo(TenantDO.class);
        tenantMapper.insert(dbTenant);// @Sql: 先插入出一条存在的数据
        TenantContextHolder.setTenantId(dbTenant.getId());

        // 调用
        tenantService.handleTenantInfo(handler);
        // 断言
        Mockito.verify(handler).handle(ArgumentMatchers.argThat(argument -> {
            AssertUtils.assertPojoEquals(dbTenant, argument);
            return true;
        }));
    }

    @Test
    public void testHandleTenantMenu_disable() {
        // 准备参数
        TenantMenuHandler handler = Mockito.mock(TenantMenuHandler.class);
        // mock 禁用
        Mockito.when(tenantProperties.getEnable()).thenReturn(false);

        // 调用
        tenantService.handleTenantMenu(handler);
        // 断言
        Mockito.verify(handler, Mockito.never()).handle(ArgumentMatchers.any());
    }

    @Test // 系统租户的情况
    public void testHandleTenantMenu_system() {
        // 准备参数
        TenantMenuHandler handler = Mockito.mock(TenantMenuHandler.class);
        // mock 未禁用
        Mockito.when(tenantProperties.getEnable()).thenReturn(true);
        // mock 租户
        TenantDO dbTenant = RandomUtils.randomPojo(TenantDO.class, o -> o.setPackageId(PACKAGE_ID_SYSTEM));
        tenantMapper.insert(dbTenant);// @Sql: 先插入出一条存在的数据
        TenantContextHolder.setTenantId(dbTenant.getId());
        // mock 菜单
        Mockito.when(menuService.getMenuList()).thenReturn(Arrays.asList(RandomUtils.randomPojo(MenuDO.class, o -> o.setId(100L)),
                RandomUtils.randomPojo(MenuDO.class, o -> o.setId(101L))));

        // 调用
        tenantService.handleTenantMenu(handler);
        // 断言
        Mockito.verify(handler).handle(asSet(100L, 101L));
    }

    @Test // 普通租户的情况
    public void testHandleTenantMenu_normal() {
        // 准备参数
        TenantMenuHandler handler = Mockito.mock(TenantMenuHandler.class);
        // mock 未禁用
        Mockito.when(tenantProperties.getEnable()).thenReturn(true);
        // mock 租户
        TenantDO dbTenant = RandomUtils.randomPojo(TenantDO.class, o -> o.setPackageId(200L));
        tenantMapper.insert(dbTenant);// @Sql: 先插入出一条存在的数据
        TenantContextHolder.setTenantId(dbTenant.getId());
        // mock 菜单
        Mockito.when(tenantPackageService.getTenantPackage(ArgumentMatchers.eq(200L))).thenReturn(RandomUtils.randomPojo(TenantPackageDO.class,
                o -> o.setMenuIds(asSet(100L, 101L))));

        // 调用
        tenantService.handleTenantMenu(handler);
        // 断言
        Mockito.verify(handler).handle(asSet(100L, 101L));
    }
}
