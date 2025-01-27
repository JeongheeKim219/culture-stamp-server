package com.culturestamp.back.dto;

import com.culturestamp.back.entity.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TagResponse {
	private Long id;
	private String tagName;

	@Builder
	public TagResponse(Tag tag) {
		this.id = tag.getId();
		this.tagName = tag.getTagName();
	}

}
