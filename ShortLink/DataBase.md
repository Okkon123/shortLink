### t_user

**表结构：**

|      字段名      |    类型    |      |     |
| :-----------: | :------: | :--: | :-: |
|      id       |  bigint  |  ID  | 主键  |
|   username    | varchar  | 用户名  |     |
|   password    | varchar  |  密码  |     |
|   real_name   | varchar  | 真实姓名 |     |
|     phone     | varchar  | 手机号  |     |
|     mail      | varchar  |  邮箱  |     |
| deletion_time |  bigint  | 注销时间 |     |
|  create_time  | datetime | 创建时间 |     |
|  update_time  | datetime | 修改时间 |     |
|   del_flag    | tinyint  | 删除标志 |     |
**索引：**

|         索引名         |    字段    |
| :-----------------: | :------: |
| idx_unique_username | username |
**分表键：** username

### t_group
**表结构：**

|     字段名     |    类型    |         |     |
| :---------: | :------: | :-----: | :-: |
|     id      |  bigint  |   ID    | 主键  |
|     gid     | varchar  |  分组标识   |     |
|    name     | varchar  |  分组名称   |     |
|  username   | varchar  | 创建分组用户名 |     |
| sort_order  | varchar  |  分组排序   |     |
| create_time | varchar  |  创建时间   |     |
| update_time |  bigint  |  修改时间   |     |
|  del_flag   | datetime |  删除标识   |     |

**索引：**

|           索引名           |      字段      |
| :---------------------: | :----------: |
| idx_unique_username_gid | gid、username |
**分表键：** username

### t_Link

**表结构：**

|       字段名       |    类型    |                      |     |
| :-------------: | :------: | :------------------: | :-: |
|       id        |  bigint  |          ID          | 主键  |
|     domain      | varchar  |          域名          |     |
|    short_uri    | varchar  |         短链接          |     |
| full_short_url  | varchar  |        完整短链接         |     |
|    orgin_url    | varchar  |         原始连接         |     |
|    click_num    |   int    |         点击量          |     |
|       gid       |  bigint  |         分组标识         |     |
|     favicon     | varchar  |         网站图标         |     |
|  enable_status  | tinyint  |   启用标识 0：启用 1：未启用    |     |
|  created_type   | tinyint  |   创建类型 0：控制台 1：接口    |     |
| valid_date_type | tinyint  | 有效期类型 0：永久有效 1：用户自定义 |     |
|   valid_date    | datetime |         有效期          |     |
|    describe     | varchar  |          描述          |     |
|    total_pv     |   int    |         历史PV         |     |
|    total_uv     |   int    |         历史UV         |     |
|    total_uip    |   int    |        历史UIP         |     |
|   create_time   | datetime |         创建时间         |     |
**索引：**

|            索引名            |       字段       |
| :-----------------------: | :------------: |
| idx_unique_full_short_url | full_short_url |
**分表键：** gid

### t_link_goto
**表结构：**

|      字段名       | 类型      |       |     |
| :------------: | ------- | :---: | :-: |
|       id       | bigint  |  ID   | 主键  |
|      gid       | varcahr | 分组标识  |     |
| full_short_url | varchar | 完整短链接 |     |

**分表键：** full_short_url

### t_link_access_logs
**表结构：**

|      字段名       |    类型    |       |     |
| :------------: | :------: | :---: | :-: |
|       id       |  bigint  |  ID   | 主键  |
| full_short_url | varchar  | 完整短链接 |     |
|      gid       | varchar  | 分组标识  |     |
|      user      | varcahr  | 用户信息  |     |
|    browser     | varchar  |  浏览器  |     |
|       os       | varchar  | 操作系统  |     |
|       ip       | varchar  |  IP   |     |
|    network     | varchar  |  网络   |     |
|     device     | varchar  | 访问设备  |     |
|     locale     | varchar  |  地区   |     |
|  create_time   | datetime | 创建时间  |     |
|  update_time   | datetime | 修改时间  |     |
|    del_flag    | tinyint  | 删除标识  |     |

### t_link_access_stats

|      字段名       |    类型    |       |     |
| :------------: | :------: | :---: | :-: |
|       id       |  bigint  |  ID   | 主键  |
|      gid       | varchar  | 分组标识  |     |
| full_short_url | varchar  | 完整短链接 |     |
|      date      |   date   |  日期   |     |
|       pv       |   int    |  访问量  |     |
|       uv       |   int    | 独立访问数 |     |
|      uip       |   int    | 独立IP  |     |
|      hour      |   int    |  小时   |     |
|    weekday     |   int    |  星期   |     |
|  create_time   | datetime | 创建时间  |     |
|  update_time   | datetime | 修改时间  |     |
|    del_flag    | tinyint  | 删除标识  |     |
**索引**

|           索引名           |              字段              |
| :---------------------: | :--------------------------: |
| idx_unique_access_stats | full_short_url、gid、date、hour |

### t_link_browser_stats
**表结构**

|      字段名       |    类型    |     |       |
| :------------: | :------: | :-: | :---: |
|       id       |  bigint  | ID  |  主键   |
| full_short_url | varchar  |     | 完整短链接 |
|      gid       | varchar  |     | 分组标识  |
|      date      |   date   |     |  日期   |
|      cnt       |   int    |     |  访问量  |
|    browser     | varchar  |     |  浏览器  |
|  create_time   | datetime |     | 创建时间  |
|  update_time   | datetime |     | 修改时间  |
|    del_flag    | tinyint  |     | 删除标识  |

**索引**

|           索引名            |               字段                |
| :----------------------: | :-----------------------------: |
| idx_unique_browser_stats | full_short_url、gid、date、browser |


### t_link_device_stats
**表结构：**

|      字段名       |    类型    |     |       |
| :------------: | :------: | :-: | :---: |
|       id       |  bigint  | ID  |  主键   |
| full_short_url | varchar  |     | 完整短链接 |
|      gid       | varchar  |     | 分组标识  |
|      date      |   date   |     |  日期   |
|      cnt       |   int    |     |  访问量  |
|     device     | varchar  |     | 访问设备  |
|  create_time   | datetime |     | 创建时间  |
|  update_time   | datetime |     | 修改时间  |
|    del_flag    | tinyint  |     | 删除标识  |

**索引**

|           索引名           |               字段               |
| :---------------------: | :----------------------------: |
| idx_unique_device_stats | full_short_url、gid、date、device |

### t_link_locale_stats
**表结构**

|      字段名       | 类型       |     |       |
| :------------: | -------- | :-: | :---: |
|       id       | bigint   | ID  |  主键   |
| full_short_url | varchar  |     | 完整短链接 |
|      gid       | varchar  |     | 分组标识  |
|      date      | date     |     |  日期   |
|      cnt       | int      |     |  访问量  |
|    province    | varchar  |     |  省份   |
|      city      | varchar  |     |  市名称  |
|     adcode     | varchar  |     | 城市编码  |
|    country     | varchar  |     |  国家   |
|  create_time   | datetime |     | 创建时间  |
|  update_time   | datetime |     | 修改时间  |
|    del_flag    | tinyint  |     | 删除标识  |

**索引**

|           索引名           |                   字段                    |
| :---------------------: | :-------------------------------------: |
| idx_unique_locale_stats | full_short_url、gid、date、adcode、province |


### t_link_network_stats

**表结构：**

|      字段名       |    类型    |     |       |
| :------------: | :------: | :-: | :---: |
|       id       |  bigint  | ID  |  主键   |
| full_short_url | varchar  |     | 完整短链接 |
|      gid       | varchar  |     | 分组标识  |
|      date      |   date   |     |  日期   |
|      cnt       |   int    |     |  访问量  |
|    network     | varchar  |     | 访问网络  |
|  create_time   | datetime |     | 创建时间  |
|  update_time   | datetime |     | 修改时间  |
|    del_flag    | tinyint  |     | 删除标识  |

**索引**

|           索引名           |               字段                |
| :---------------------: | :-----------------------------: |
| idx_unique_device_stats | full_short_url、gid、date、network |

### t_link_os_stats

**表结构：**

|      字段名       |    类型    |     |       |
| :------------: | :------: | :-: | :---: |
|       id       |  bigint  | ID  |  主键   |
| full_short_url | varchar  |     | 完整短链接 |
|      gid       | varchar  |     | 分组标识  |
|      date      |   date   |     |  日期   |
|      cnt       |   int    |     |  访问量  |
|       os       | varchar  |     | 操作系统  |
|  create_time   | datetime |     | 创建时间  |
|  update_time   | datetime |     | 修改时间  |
|    del_flag    | tinyint  |     | 删除标识  |

**索引**

|         索引名         |             字段             |
| :-----------------: | :------------------------: |
| idx_unique_os_stats | full_short_url、gid、date、os |

### t_link_stats_today
**表结构：**

|      字段名       |    类型    |       |     |
| :------------: | :------: | :---: | :-: |
|       id       |  bigint  |  ID   | 主键  |
|      gid       | varchar  | 分组标识  |     |
| full_short_url | varchar  |  短链接  |     |
|      date      |   date   |  日期   |     |
|    today_pv    |   int    | 今日PV  |     |
|    today_uv    |   int    | 今日UV  |     |
|   today_uip    |   int    | 今日IP数 |     |
|  create_time   | datetime | 创建时间  |     |
|  update_time   | datetime | 修改时间  |     |
|    del_flag    | tinyint  | 删除标识  |     |

**索引**

|          索引名           |           字段            |
| :--------------------: | :---------------------: |
| idx_unique_today_stats | full_short_url、gid、date |
