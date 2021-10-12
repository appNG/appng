package org.appng.api.support.validation.model;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.appng.api.NotBlank;

import lombok.Data;

@Data
public class Person {
	private Integer id;
	private @NotBlank String name;
	private @NotBlank String lastname;
	private @Valid List<Address> addresses = new ArrayList<>();
}
