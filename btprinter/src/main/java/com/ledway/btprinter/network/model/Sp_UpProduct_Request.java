package com.ledway.btprinter.network.model;

/*
@Line int,
@Reader int,
@empno nvarchar(20)  ,--員工代碼,輸入Mac No,不可爲空
@prodno nvarchar(255),--布種編碼,不可爲空
@specdesc nvarchar(1000), -- 布种描述
@graphic image,--布種圖片,不可爲空，多張圖片請拼接
@graphic2 image,--布種縮略圖110X110,不可爲空
@errCode int output,
@errData nvarchar(50) output
*/

public class Sp_UpProduct_Request extends BaseRequest{ ;
  public String empno;
  public String prodno;
  public String specdesc;
  public String graphic;
  public String graphic2;
}
