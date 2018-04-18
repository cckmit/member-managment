package com.mmiholdings.member.money.api;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LinkCardRequest {
	private final String accountReferenceToLinkCardTo;
	private final String accountReferenceCardIsLinkedTo;
	private final String cardReference;
	private final String userId;
}
