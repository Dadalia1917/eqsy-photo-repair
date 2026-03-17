package com.ruoyi.common.core.domain.model;

/**
 * 用户注册对象
 * 
 * @author ruoyi
 */
public class RegisterBody extends LoginBody
{
	/**
	 * 手机号
	 */
	private String phonenumber;

	/**
	 * 短信验证码
	 */
	private String smsCode;

	public String getPhonenumber()
	{
		return phonenumber;
	}

	public void setPhonenumber(String phonenumber)
	{
		this.phonenumber = phonenumber;
	}

	public String getSmsCode()
	{
		return smsCode;
	}

	public void setSmsCode(String smsCode)
	{
		this.smsCode = smsCode;
	}

}
