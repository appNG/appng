package org.appng.api.support.validation.model;

import org.appng.api.NotBlank;

import lombok.Data;

@Data
public class Location {
	private @NotBlank String zip;
	private @NotBlank String city;
}
