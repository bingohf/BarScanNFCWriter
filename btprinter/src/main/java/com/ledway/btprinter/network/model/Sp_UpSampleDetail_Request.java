package com.ledway.btprinter.network.model;

/*
CREATE proc [dbo].[sp_UpSampleDetail]
@Line int,
@Reader int,
@empno nvarchar(20)  ,--員工代碼,輸入Mac No,不可爲空
@series nvarchar(50) ,--取樣單series,不可爲空
@prodno nvarchar(50),--布種,不可爲空
@itemExt nvarchar(10),--序號,不可爲空
@pcsnum int,--片數,不可爲空,默認爲1即可
@errCode int output,
@errData nvarchar(50) output,
@outProdno nvarchar(50) output--如果此輸出參數有值則需要給布種拍照,調用sp_UpProduct存儲過程提交圖片以及縮略圖
    As
    Begin
*/

public class Sp_UpSampleDetail_Request {
  public int line;
  public int reader;
  public String empno;
  public String series;
  public String prodno;
  public String itemExt;
  public int pcsnum;
}
