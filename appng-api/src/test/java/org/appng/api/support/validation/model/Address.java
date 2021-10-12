package org.appng.api.support.validation.model;

import javax.validation.Valid;

import org.appng.api.NotBlank;

import lombok.Data;

@Data
public class Address {
	private @NotBlank String addressId = "42";
	private @Valid Location location = new Location();
}
