# kg core api

## get document with fieldname "describedType" of type <typeinfo> from client space


## Arango Documents
```plantuml
database "Arango" {
  folder "In_progress space" as f1 {
    frame "Document2" as D2 {
      note as N2
        @id=893483984-493993
        @type=https://schema.hbp.eu/minds/person
        name=Katrin Amunts
      end note
  	}
	frame "Document1" as D1 {
      note as N1
        @id=4324234-493933
        @type=https://schema.hbp.eu/uniminds/dataset
        name=3D Cell
        contributors=[
        	{@id=893483984-493993}
            ...
        ]
      end note
  	}
  }
  
  folder "User space" as f2 {
    frame "Document6" as D6 {
    	note as N6
			@id=4477473
			@type=https:///schema.hbp.eu/user
			name=Oliver S.
		end note
  	}
  }
  
  folder "<Editor> client space" as f3 {
    frame "Document4" as D4 {
    	note as N4
          @id=34534533
          @type=https://schema.hbp.eu/bookmark
          name=My Favorites 1
          user={@id=4477473}
          list=[
                {@id=4324234-493933},
                {@id=893483984-493993}
            	...
            ]
		end note
  	}
	frame "Document3" as D3 {
        note as N3
        	@id=345344433
        	@type=https://schema.hbp.eu/typeInfo
        	describedType={
                @id=https://schema.hbp.eu/uniminds/dataset
            }
        	labelField=name
        	promotedFields=[
            	{
                    @id=https://schema.hbp.eu/name
                }
            ]
		end note
  	}
  }
}

f3 -down- f2 #white
f2 -down- f1 #white
```