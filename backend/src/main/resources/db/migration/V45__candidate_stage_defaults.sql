UPDATE crm_custom_field
SET options = '[{"value":"new","label":"新候选人"},{"value":"screening","label":"初筛"},{"value":"interview_scheduled","label":"安排面试"},{"value":"interview_passed","label":"面试通过"},{"value":"offer","label":"Offer"},{"value":"hired","label":"已入职"},{"value":"rejected","label":"已淘汰"}]',
    update_time = CURRENT_TIMESTAMP
WHERE entity_type = 'candidate'
  AND field_name = 'stage'
  AND (
      options IS NULL
      OR options NOT LIKE '%interview_scheduled%'
      OR options NOT LIKE '%interview_passed%'
  );
